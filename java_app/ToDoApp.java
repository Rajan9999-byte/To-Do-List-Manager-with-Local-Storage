// ============================================================
// ToDoApp.java — To-Do List Manager Console Application
// HOW TO COMPILE: javac ToDoApp.java
// HOW TO RUN:     java ToDoApp
// LOCAL STORAGE:  saves tasks to data/tasks.json (CSV format)
// ============================================================

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

public class ToDoApp {

  // ── Enums ─────────────────────────────────────────────────
  enum Priority  { LOW, MEDIUM, HIGH, CRITICAL }
  enum Status    { PENDING, IN_PROGRESS, COMPLETED }
  enum Category  { PERSONAL, STUDY, WORK, HEALTH, SHOPPING, OTHER }

  // ── Task Model ────────────────────────────────────────────
  static class Task {
    String   id;
    String   title;
    String   description;
    Priority priority;
    Category category;
    Status   status;
    String   dueDate;
    String   createdAt;
    String   updatedAt;

    Task(String title, String description,
         Priority priority, Category category, String dueDate) {
      this.id          = "T" + System.currentTimeMillis() % 100000;
      this.title       = title;
      this.description = description;
      this.priority    = priority;
      this.category    = category;
      this.status      = Status.PENDING;
      this.dueDate     = dueDate;
      this.createdAt   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
      this.updatedAt   = this.createdAt;
    }

    // Constructor for loading from CSV
    Task(String id, String title, String description,
         Priority priority, Category category, Status status,
         String dueDate, String createdAt, String updatedAt) {
      this.id=id; this.title=title; this.description=description;
      this.priority=priority; this.category=category; this.status=status;
      this.dueDate=dueDate; this.createdAt=createdAt; this.updatedAt=updatedAt;
    }

    boolean isOverdue() {
      if (dueDate == null || dueDate.isEmpty() || status == Status.COMPLETED) return false;
      try {
        LocalDate due   = LocalDate.parse(dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return due.isBefore(LocalDate.now());
      } catch (Exception e) { return false; }
    }

    String toCSV() {
      return String.join("|",
        id, title.replace("|","_"), description.replace("|","_"),
        priority.name(), category.name(), status.name(),
        dueDate, createdAt, updatedAt);
    }

    static Task fromCSV(String line) {
      String[] p = line.split("\\|", -1);
      if (p.length < 9) return null;
      try {
        return new Task(
          p[0], p[1], p[2],
          Priority.valueOf(p[3]),
          Category.valueOf(p[4]),
          Status.valueOf(p[5]),
          p[6], p[7], p[8]
        );
      } catch (Exception e) { return null; }
    }

    @Override
    public String toString() {
      String overdue = isOverdue() ? " [OVERDUE!]" : "";
      return String.format("  [%s] %-6s | %-8s | %-8s | %-10s | Due: %-12s | %s%s",
        id, status.name().substring(0,Math.min(6,status.name().length())),
        priority.name(), category.name(), "",
        dueDate.isEmpty()?"—":dueDate, title, overdue);
    }
  }

  // ── Data File ─────────────────────────────────────────────
  static final String DATA_DIR  = "data";
  static final String DATA_FILE = DATA_DIR + File.separator + "tasks.csv";
  static List<Task>   tasks     = new ArrayList<>();
  static Scanner      sc        = new Scanner(System.in);

  // ── Load from file ────────────────────────────────────────
  static void loadTasks() {
    File dir = new File(DATA_DIR);
    if (!dir.exists()) dir.mkdirs();

    File f = new File(DATA_FILE);
    if (!f.exists()) { System.out.println("  → No saved tasks found. Starting fresh."); return; }

    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
      String line;
      int loaded = 0;
      while ((line = br.readLine()) != null) {
        if (line.trim().isEmpty() || line.startsWith("#")) continue;
        Task t = Task.fromCSV(line);
        if (t != null) { tasks.add(t); loaded++; }
      }
      System.out.println("  ✅ Loaded " + loaded + " tasks from local storage.");
    } catch (IOException e) {
      System.out.println("  ⚠️ Could not read tasks file: " + e.getMessage());
    }
  }

  // ── Save to file ──────────────────────────────────────────
  static void saveTasks() {
    try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
      pw.println("# TaskFlow Local Storage — do not edit manually");
      for (Task t : tasks) pw.println(t.toCSV());
    } catch (IOException e) {
      System.out.println("  ⚠️ Could not save tasks: " + e.getMessage());
    }
  }

  // ── Main ──────────────────────────────────────────────────
  public static void main(String[] args) {
    printBanner();
    loadTasks();
    menuLoop();
  }

  static void printBanner() {
    System.out.println();
    System.out.println("╔═══════════════════════════════════════════╗");
    System.out.println("║     TaskFlow — To-Do List Manager          ║");
    System.out.println("║     Java Console | Local Storage           ║");
    System.out.println("╚═══════════════════════════════════════════╝");
    System.out.println();
  }

  static void menuLoop() {
    while (true) {
      long overdue = tasks.stream().filter(Task::isOverdue).count();
      long pending = tasks.stream().filter(t -> t.status != Status.COMPLETED).count();

      System.out.println("\n══════════════════ MENU ══════════════════");
      System.out.printf("  Tasks: %d total | %d pending | %d overdue%n",
        tasks.size(), pending, overdue);
      System.out.println("  1. ➕ Add Task");
      System.out.println("  2. 📋 View All Tasks");
      System.out.println("  3. ✏️  Edit Task");
      System.out.println("  4. ✅ Mark Completed");
      System.out.println("  5. 🔄 Mark Pending");
      System.out.println("  6. 🗑️  Delete Task");
      System.out.println("  7. 🔍 Search Tasks");
      System.out.println("  8. 🔽 Filter by Status");
      System.out.println("  9. 🔼 Sort by Priority");
      System.out.println(" 10. ⚠️  View Overdue Tasks");
      System.out.println(" 11. 📊 Summary Report");
      System.out.println(" 12. 🚪 Save & Exit");
      System.out.println("══════════════════════════════════════════");

      int choice = readInt("Choice", 1, 12);
      switch (choice) {
        case 1  -> addTask();
        case 2  -> viewAll();
        case 3  -> editTask();
        case 4  -> markStatus(Status.COMPLETED);
        case 5  -> markStatus(Status.PENDING);
        case 6  -> deleteTask();
        case 7  -> searchTasks();
        case 8  -> filterByStatus();
        case 9  -> sortByPriority();
        case 10 -> viewOverdue();
        case 11 -> printSummary();
        case 12 -> { saveTasks(); System.out.println("\nGoodbye! Data saved. 👋"); return; }
      }
    }
  }

  static void addTask() {
    System.out.println("\n── Add New Task ──────────────────────────");
    String title = readNonEmpty("Title");
    System.out.print("  Description (optional): ");
    String desc = sc.nextLine().trim();

    Priority pri = pickEnum("Priority", Priority.values());
    Category cat = pickEnum("Category", Category.values());

    System.out.print("  Due date (yyyy-MM-dd, or leave blank): ");
    String due = sc.nextLine().trim();

    Task t = new Task(title, desc, pri, cat, due);
    tasks.add(t);
    saveTasks();
    System.out.println("  ✅ Task added! ID: " + t.id);
  }

  static void viewAll() {
    if (tasks.isEmpty()) { System.out.println("  No tasks yet."); return; }
    System.out.println("\n── All Tasks ─────────────────────────────");
    tasks.forEach(System.out::println);
  }

  static void editTask() {
    String id = readNonEmpty("Task ID to edit");
    Task t = findById(id);
    if (t == null) { System.out.println("  ❌ Task not found."); return; }

    System.out.println("  Current title: " + t.title);
    System.out.print("  New title (Enter to keep): ");
    String nt = sc.nextLine().trim();
    if (!nt.isEmpty()) t.title = nt;

    System.out.print("  New due date (Enter to keep [" + t.dueDate + "]): ");
    String nd = sc.nextLine().trim();
    if (!nd.isEmpty()) t.dueDate = nd;

    t.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    saveTasks();
    System.out.println("  ✅ Task updated.");
  }

  static void markStatus(Status newStatus) {
    String id = readNonEmpty("Task ID");
    Task t = findById(id);
    if (t == null) { System.out.println("  ❌ Not found."); return; }
    t.status    = newStatus;
    t.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    saveTasks();
    System.out.println("  ✅ Status updated to " + newStatus);
  }

  static void deleteTask() {
    String id = readNonEmpty("Task ID to delete");
    Task t = findById(id);
    if (t == null) { System.out.println("  ❌ Not found."); return; }
    System.out.print("  Delete '" + t.title + "'? (y/n): ");
    if (!sc.nextLine().trim().equalsIgnoreCase("y")) return;
    tasks.remove(t);
    saveTasks();
    System.out.println("  ✅ Deleted.");
  }

  static void searchTasks() {
    System.out.print("  Search: ");
    String q = sc.nextLine().trim().toLowerCase();
    List<Task> results = tasks.stream()
      .filter(t -> t.title.toLowerCase().contains(q) ||
                   t.description.toLowerCase().contains(q))
      .collect(Collectors.toList());
    if (results.isEmpty()) { System.out.println("  No matches."); return; }
    results.forEach(System.out::println);
  }

  static void filterByStatus() {
    Status s = pickEnum("Filter by status", Status.values());
    tasks.stream().filter(t -> t.status == s).forEach(System.out::println);
  }

  static void sortByPriority() {
    System.out.println("\n── Tasks sorted by priority (Critical→Low) ──");
    tasks.stream()
      .sorted(Comparator.comparingInt((Task t) -> t.priority.ordinal()).reversed())
      .forEach(System.out::println);
  }

  static void viewOverdue() {
    System.out.println("\n── Overdue Tasks ────────────────────────────");
    List<Task> od = tasks.stream().filter(Task::isOverdue).collect(Collectors.toList());
    if (od.isEmpty()) System.out.println("  ✅ No overdue tasks!");
    else od.forEach(System.out::println);
  }

  static void printSummary() {
    System.out.println("\n══════════════ SUMMARY REPORT ══════════════");
    System.out.println("  Total tasks   : " + tasks.size());
    System.out.println("  Pending       : " + tasks.stream().filter(t->t.status==Status.PENDING).count());
    System.out.println("  In Progress   : " + tasks.stream().filter(t->t.status==Status.IN_PROGRESS).count());
    System.out.println("  Completed     : " + tasks.stream().filter(t->t.status==Status.COMPLETED).count());
    System.out.println("  Overdue       : " + tasks.stream().filter(Task::isOverdue).count());
    System.out.println("\n  By Priority:");
    for (Priority p : Priority.values())
      System.out.printf("    %-10s: %d%n", p, tasks.stream().filter(t->t.priority==p).count());
    System.out.println("\n  By Category:");
    for (Category c : Category.values())
      System.out.printf("    %-10s: %d%n", c, tasks.stream().filter(t->t.category==c).count());
    System.out.println("════════════════════════════════════════════");
  }

  // ── Helpers ───────────────────────────────────────────────
  static Task findById(String id) {
    return tasks.stream().filter(t -> t.id.equalsIgnoreCase(id)).findFirst().orElse(null);
  }

  static String readNonEmpty(String prompt) {
    while (true) {
      System.out.print("  " + prompt + ": ");
      String s = sc.nextLine().trim();
      if (!s.isEmpty()) return s;
      System.out.println("  ⚠️ Cannot be empty.");
    }
  }

  static int readInt(String prompt, int min, int max) {
    while (true) {
      System.out.print("  " + prompt + " (" + min + "-" + max + "): ");
      try {
        int v = Integer.parseInt(sc.nextLine().trim());
        if (v >= min && v <= max) return v;
      } catch (NumberFormatException ignored) {}
      System.out.println("  ⚠️ Enter a number between " + min + " and " + max);
    }
  }

  static <T extends Enum<T>> T pickEnum(String prompt, T[] values) {
    System.out.println("  " + prompt + ":");
    for (int i = 0; i < values.length; i++)
      System.out.printf("    %d. %s%n", i+1, values[i]);
    return values[readInt("Choice", 1, values.length) - 1];
  }
}