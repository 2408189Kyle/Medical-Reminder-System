import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Medicine Reminder System
 *
 * Single-file Java console app demonstrating OOP principles, CRUD, scheduling reminders,
 * exception handling, and user interaction via console.
 *
 * Save as Main.java, compile: javac Main.java
 * Run: java Main
 *
 * Notes:
 * - Schedule reminders by entering time in HH:mm format (24-hour).
 * - Reminders are printed to the console (ConsoleReminder). You can extend Notification types
 * (e.g., EmailReminder) by subclassing Reminder and overriding notifyUser().
 */
public class Main {
    // Manager that holds medicine objects and handles CRUD
    static class MedicineManager {
        private final List<Medicine> medicines = new ArrayList<>();

        // Add medicine
        public void addMedicine(Medicine m) {
            medicines.add(m);
        }

        // Get by id (index)
        public Medicine getMedicine(int index) {
            if (index < 0 || index >= medicines.size()) return null;
            return medicines.get(index);
        }

        // Update medicine at index
        public boolean updateMedicine(int index, Medicine updated) {
            if (index < 0 || index >= medicines.size()) return false;
            medicines.set(index, updated);
            return true;
        }

        // Delete medicine
        public boolean deleteMedicine(int index) {
            if (index < 0 || index >= medicines.size()) return false;
            medicines.remove(index);
            return true;
        }

        // List all
        public List<Medicine> listAll() {
            return Collections.unmodifiableList(medicines);
        }

        public int size() {
            return medicines.size();
        }
    }

    // Simple POJO for Medicine (encapsulation: private fields + getters/setters)
    static class Medicine {
        private String name;
        private String dose; // e.g. "500mg"
        private String notes;

        // A list of scheduled reminder times for this medicine (HH:mm strings)
        private final List<String> reminderTimes = new ArrayList<>();

        public Medicine(String name, String dose, String notes) {
            this.name = name;
            this.dose = dose;
            this.notes = notes;
        }

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDose() { return dose; }
        public void setDose(String dose) { this.dose = dose; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public List<String> getReminderTimes() {
            return reminderTimes;
        }

        public void addReminderTime(String hhmm) {
            reminderTimes.add(hhmm);
        }

        public void removeReminderTime(int idx) {
            if (idx >= 0 && idx < reminderTimes.size()) reminderTimes.remove(idx);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Name: ").append(name).append("\n");
            sb.append("Dose: ").append(dose).append("\n");
            sb.append("Notes: ").append(notes).append("\n");
            if (reminderTimes.isEmpty()) {
                sb.append("Reminders: (none)\n");
            } else {
                sb.append("Reminders: ").append(String.join(", ", reminderTimes)).append("\n");
            }
            return sb.toString();
        }
    }

    // Abstract Reminder (demonstrates abstraction). Subclasses implement notifyUser().
    static abstract class Reminder implements Runnable {
        protected final Medicine medicine;
        protected final String timeStr; // HH:mm

        public Reminder(Medicine medicine, String timeStr) {
            this.medicine = medicine;
            this.timeStr = timeStr;
        }

        // When the scheduled time arrives, run() will call notifyUser()
        @Override
        public void run() {
            notifyUser();
        }

        // Concrete subclasses provide notification behavior (Polymorphism)
        protected abstract void notifyUser();
    }

    // Concrete console reminder (Inheritance and Polymorphism)
    static class ConsoleReminder extends Reminder {
        public ConsoleReminder(Medicine medicine, String timeStr) {
            super(medicine, timeStr);
        }

        @Override
        protected void notifyUser() {
            String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            System.out.println("====================================");
            System.out.println("MEDICINE REMINDER (" + timeNow + ")");
            System.out.println("Take medicine: " + medicine.getName());
            System.out.println("Dose: " + medicine.getDose());
            System.out.println("Notes: " + medicine.getNotes());
            System.out.println("Scheduled time: " + timeStr);
            System.out.println("====================================");
        }
    }

    // Scheduler utility: schedules daily reminders at specified HH:mm times
    static class ReminderScheduler {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
        private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        // generate a unique key for a medicine/time pair
        private String key(Medicine m, String hhmm) {
            return m.getName() + "@" + hhmm;
        }

        // Schedule a daily reminder at HH:mm (24-hour)
        public void scheduleDaily(Medicine m, String hhmm) throws DateTimeParseException {
            LocalTime target = LocalTime.parse(hhmm, timeFmt);
            long initialDelay = computeInitialDelaySeconds(target);
            long oneDay = Duration.ofDays(1).getSeconds();

            String k = key(m, hhmm);
            // cancel existing if present
            if (scheduledTasks.containsKey(k)) {
                scheduledTasks.get(k).cancel(false);
            }

            // scheduleAtFixedRate with delay in seconds
            ScheduledFuture<?> sf = scheduler.scheduleAtFixedRate(
                    new ConsoleReminder(m, hhmm), // Polymorphism: Reminder reference holding ConsoleReminder instance
                    initialDelay,
                    oneDay,
                    TimeUnit.SECONDS
            );
            scheduledTasks.put(k, sf);
            System.out.println("Scheduled reminder for " + m.getName() + " at " + hhmm + " daily.");
        }

        // Cancel a scheduled reminder
        public boolean cancel(Medicine m, String hhmm) {
            String k = key(m, hhmm);
            ScheduledFuture<?> sf = scheduledTasks.remove(k);
            if (sf != null) {
                boolean cancelled = sf.cancel(false);
                if (cancelled) System.out.println("Cancelled reminder: " + k);
                return cancelled;
            }
            return false;
        }

        // Shutdown scheduler (call on program exit)
        public void shutdown() {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        // Compute seconds from now until the next occurrence of target time
        private long computeInitialDelaySeconds(LocalTime target) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = now.withHour(target.getHour()).withMinute(target.getMinute()).withSecond(0).withNano(0);
            if (!next.isAfter(now)) {
                next = next.plusDays(1);
            }
            return Duration.between(now, next).getSeconds();
        }
    }

    // Console UI and main
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        MedicineManager manager = new MedicineManager();
        ReminderScheduler scheduler = new ReminderScheduler();

        // Sample data (optional)
        manager.addMedicine(new Medicine("Paracetamol", "500mg", "Take after meal"));
        manager.getMedicine(0).addReminderTime("09:00"); // sample reminder time
        try {
            scheduler.scheduleDaily(manager.getMedicine(0), "09:00");
        } catch (DateTimeParseException e) {
            // ignore in sample
        }

        boolean running = true;
        System.out.println("=== Medicine Reminder System ===");
        while (running) {
            printMenu();
            System.out.print("Choose option: ");
            String opt = sc.nextLine().trim();
            switch (opt) {
                case "1": // Add
                    handleAdd(sc, manager, scheduler);
                    break;
                case "2": // View
                    handleView(sc, manager);
                    break;
                case "3": // Update
                    handleUpdate(sc, manager, scheduler);
                    break;
                case "4": // Delete
                    handleDelete(sc, manager, scheduler);
                    break;
                case "5": // List all
                    handleList(manager);
                    break;
                case "6": // Exit
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }

        System.out.println("Shutting down scheduler and exiting...");
        scheduler.shutdown();
        sc.close();
    }

    private static void printMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. Add medicine");
        System.out.println("2. View medicine (by index)");
        System.out.println("3. Update medicine");
        System.out.println("4. Delete medicine");
        System.out.println("5. List all medicines");
        System.out.println("6. Exit");
    }

    private static void handleAdd(Scanner sc, MedicineManager manager, ReminderScheduler scheduler) {
        try {
            System.out.print("Medicine name: ");
            String name = nonEmptyLine(sc);
            System.out.print("Dose (e.g., 500mg): ");
            String dose = nonEmptyLine(sc);
            System.out.print("Notes: ");
            String notes = sc.nextLine().trim();

            Medicine m = new Medicine(name, dose, notes);
            manager.addMedicine(m);
            System.out.println("Medicine added with index " + (manager.size() - 1));

            // Ask to add reminder times
            while (true) {
                System.out.print("Add a reminder time for this medicine? (y/n): ");
                String yn = sc.nextLine().trim().toLowerCase();
                if (yn.equals("y")) {
                    System.out.print("Enter time (HH:mm 24-hour): ");
                    String hhmm = sc.nextLine().trim();
                    try {
                        // validate format
                        LocalTime.parse(hhmm, DateTimeFormatter.ofPattern("HH:mm"));
                        m.addReminderTime(hhmm);
                        scheduler.scheduleDaily(m, hhmm);
                    } catch (DateTimeParseException ex) {
                        System.out.println("Invalid time format. Use HH:mm.");
                    }
                } else break;
            }
        } catch (Exception e) {
            System.out.println("Error adding medicine: " + e.getMessage());
        }
    }

    private static void handleView(Scanner sc, MedicineManager manager) {
        if (manager.size() == 0) {
            System.out.println("No medicines yet.");
            return;
        }
        System.out.print("Enter medicine index (0 - " + (manager.size()-1) + "): ");
        String sindex = sc.nextLine().trim();
        try {
            int idx = Integer.parseInt(sindex);
            Medicine m = manager.getMedicine(idx);
            if (m == null) {
                System.out.println("No medicine at that index.");
            } else {
                System.out.println("---- Medicine ----");
                System.out.println(m);
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    private static void handleUpdate(Scanner sc, MedicineManager manager, ReminderScheduler scheduler) {
        if (manager.size() == 0) {
            System.out.println("No medicines to update.");
            return;
        }
        System.out.print("Enter medicine index to update (0 - " + (manager.size()-1) + "): ");
        String sindex = sc.nextLine().trim();
        try {
            int idx = Integer.parseInt(sindex);
            Medicine m = manager.getMedicine(idx);
            if (m == null) {
                System.out.println("Invalid index.");
                return;
            }
            System.out.println("Current info:");
            System.out.println(m);

            System.out.print("New name (leave blank to keep): ");
            String newName = sc.nextLine().trim();
            if (!newName.isEmpty()) m.setName(newName);

            System.out.print("New dose (leave blank to keep): ");
            String newDose = sc.nextLine().trim();
            if (!newDose.isEmpty()) m.setDose(newDose);

            System.out.print("New notes (leave blank to keep): ");
            String newNotes = sc.nextLine().trim();
            if (!newNotes.isEmpty()) m.setNotes(newNotes);

            // Manage reminders for this medicine
            manageRemindersForMedicine(sc, m, scheduler);

            System.out.println("Medicine updated.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }

    private static void handleDelete(Scanner sc, MedicineManager manager, ReminderScheduler scheduler) {
        if (manager.size() == 0) {
            System.out.println("No medicines to delete.");
            return;
        }
        System.out.print("Enter medicine index to delete (0 - " + (manager.size()-1) + "): ");
        String sindex = sc.nextLine().trim();
        try {
            int idx = Integer.parseInt(sindex);
            Medicine m = manager.getMedicine(idx);
            if (m == null) {
                System.out.println("Invalid index.");
                return;
            }

            // Cancel scheduled reminders for this medicine
            for (String hhmm : new ArrayList<>(m.getReminderTimes())) {
                scheduler.cancel(m, hhmm);
            }

            manager.deleteMedicine(idx);
            System.out.println("Medicine deleted.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
        }
    }

    private static void handleList(MedicineManager manager) {
        List<Medicine> all = manager.listAll();
        if (all.isEmpty()) {
            System.out.println("No medicines yet.");
            return;
        }
        System.out.println("---- All Medicines ----");
        for (int i = 0; i < all.size(); i++) {
            System.out.println("Index " + i + ": " + all.get(i).getName() + " (" + all.get(i).getDose() + ")");
        }
    }

    // Helper to manage reminders for a medicine: add/remove times
    private static void manageRemindersForMedicine(Scanner sc, Medicine m, ReminderScheduler scheduler) {
        while (true) {
            System.out.println("Reminder times: " + (m.getReminderTimes().isEmpty() ? "(none)" : m.getReminderTimes()));
            System.out.println("a) Add reminder time");
            System.out.println("b) Remove reminder time");
            System.out.println("c) Done");
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim().toLowerCase();
            if (ch.equals("a")) {
                System.out.print("Enter time (HH:mm): ");
                String hhmm = sc.nextLine().trim();
                try {
                    LocalTime.parse(hhmm, DateTimeFormatter.ofPattern("HH:mm"));
                    m.addReminderTime(hhmm);
                    scheduler.scheduleDaily(m, hhmm);
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid time format.");
                }
            } else if (ch.equals("b")) {
                if (m.getReminderTimes().isEmpty()) {
                    System.out.println("No reminder times to remove.");
                    continue;
                }
                for (int i = 0; i < m.getReminderTimes().size(); i++) {
                    System.out.println(i + ": " + m.getReminderTimes().get(i));
                }
                System.out.print("Enter index to remove: ");
                String idxStr = sc.nextLine().trim();
                try {
                    int ridx = Integer.parseInt(idxStr);
                    if (ridx < 0 || ridx >= m.getReminderTimes().size()) {
                        System.out.println("Invalid index.");
                    } else {
                        String hhmm = m.getReminderTimes().get(ridx);
                        boolean cancelled = scheduler.cancel(m, hhmm);
                        m.removeReminderTime(ridx);
                        if (!cancelled) System.out.println("Reminder removed from memory, but scheduler had no record.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number.");
                }
            } else if (ch.equals("c")) {
                break;
            } else {
                System.out.println("Unknown option.");
            }
        }
    }

    // Utility: read a non-empty line
    private static String nonEmptyLine(Scanner sc) {
        String s;
        do {
            s = sc.nextLine().trim();
            if (s.isEmpty()) System.out.print("Please enter a non-empty value: ");
        } while (s.isEmpty());
        return s;
    }
}