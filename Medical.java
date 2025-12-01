import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Medical Reminder System
 * 
 * Save this file as Medical.java
 * Compile: javac Medical.java
 * Run: java Medical
 */
public class Medical {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ============================================================================
    // MAIN METHOD - Entry point of the program
    // ============================================================================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Medicine> meds = new ArrayList<>();
        Patient currentPatient = null;
        
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║    MEDICAL REMINDER SYSTEM             ║");
        System.out.println("╚════════════════════════════════════════╝");

        System.out.print("\n  Full Name: ");
        String fullName = sc.nextLine();
        
        int age = 0;
        while(age <= 0) {
            System.out.print("  Age: ");
            try {
                age = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                age = 0;
            }
        }
        
        String gender = "";
        while (gender.isEmpty()) {
            System.out.print("  Gender (M/F): ");
            String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("M") || input.equals("F")) {
                gender = input.equals("M") ? "Male" : "Female";
            }
        }

        currentPatient = new Patient(fullName, age, gender);
        currentPatient.displayInfo();

        int choice = -1;
        while (choice != 9) {
            printMenu();
            System.out.print("  Choose option: ");

            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Invalid input");
                continue;
            }

            switch (choice) {
                case 1:
                    Medicine newMed = createMedicine(sc);
                    meds.add(newMed);
                    System.out.println("\n  ✓ Prescription added and scheduled!");
                    System.out.println("  The first dose is set for: " + newMed.getNextTakeTime().format(Medicine.FORMATTER));
                    displayAgeRecommendation(age);
                    break;

                case 2:
                    System.out.println("\n  ╔════ UPCOMING DOSE SCHEDULE ════╗");
                    if (meds.isEmpty()) {
                        System.out.println("  ║ No prescriptions yet           ║");
                    } else {
                        meds.sort(Comparator.comparing(Medicine::getIsCompleted)
                                            .thenComparing(Medicine::getIsActive, Comparator.reverseOrder())
                                            .thenComparing(Medicine::getNextTakeTime));
                                            
                        System.out.println("  ╠═══════════════════════════════════════════╣");
                        System.out.println("  ║ No │ Name         │ Next Dose Time     │ Status  ║");
                        System.out.println("  ╠═══════════════════════════════════════════╣");

                        LocalDateTime now = LocalDateTime.now();

                        for (int i = 0; i < meds.size(); i++) {
                            Medicine m = meds.get(i);
                            String status = m.getStatusDisplay();
                            String nextTimeStr = m.getNextTakeTime().format(Medicine.FORMATTER);
                            
                            String indicator = "";
                            if (status.equals("OVERDUE") || (m.getNextTakeTime().isBefore(now.plusHours(2)) && m.getIsActive())) {
                                 indicator = " *DUE*"; 
                            }

                            System.out.printf("  ║ %2d │ %-13s│ %-18s │ %-7s%s║\n", 
                                                (i + 1), m.getName(), nextTimeStr, status, 
                                                String.format("%-5s", indicator).substring(0, 5));
                        }
                    }
                    System.out.println("  ╚═══════════════════════════════════════════╝");
                    break;
                
                case 3:
                    takeAndRecalculate(sc, meds);
                    break;

                case 4:
                    updateMedicineStatus(sc, meds);
                    break;

                case 5:
                    calculateMedicineUsage(sc, meds);
                    break;

                case 6:
                    displayAgeRecommendation(age);
                    break;

                case 7:
                    viewPrescriptionDetails(sc, meds);
                    break;

                case 8:
                    searchMedicine(sc, meds);
                    break;

                case 9:
                    System.out.println("\n  Thank you for using Medical Reminder System!");
                    System.out.println("  Goodbye!\n");
                    break;

                default:
                    System.out.println("  ✗ Invalid choice");
                    break;
            }
        }
        sc.close();
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private static Medicine createMedicine(Scanner sc) {
        System.out.println("\n  ╔═══ NEW PRESCRIPTION ═══╗");
        
        System.out.print("  Medicine Name: ");
        String name = nonEmptyLine(sc);

        System.out.print("  Dosage (e.g., 500mg): ");
        String dosage = nonEmptyLine(sc);

        System.out.print("  Frequency (e.g., '8 hours' or '1 day'): ");
        String frequency = nonEmptyLine(sc);

        System.out.print("  Notes (e.g., 'Take with food' or 'Before meals'): ");
        String notes = sc.nextLine().trim();

        LocalDateTime initialTime = null;
        while (initialTime == null) {
            System.out.println("\n  ├─ Initial Dose Time");
            System.out.print("  │ Date (DD/MM/YYYY): ");
            String dateStr = sc.nextLine();

            System.out.print("  │ Time (HH:MM): ");
            String timeStr = sc.nextLine();
            
            String combinedStr = dateStr + " " + timeStr;

            try {
                initialTime = LocalDateTime.parse(combinedStr, INPUT_FORMATTER);
                if (initialTime.isBefore(LocalDateTime.now())) {
                    System.out.println("  └─ Note: Time is in the past. Proceeding...");
                }
            } catch (DateTimeParseException e) {
                System.out.println("  └─ Invalid format. Use DD/MM/YYYY HH:MM");
            }
        }
        
        long durationDays = -1; 
        while(true) {
            System.out.print("\n  Duration in days (0 for continuous): ");
            String durationStr = sc.nextLine().trim();

            if (durationStr.isEmpty()) {
                durationStr = "0";
            }
            
            try {
                durationDays = Long.parseLong(durationStr);
                if (durationDays >= 0) {
                    break;
                } else {
                    System.out.println("  → Must be 0 or positive number");
                }
            } catch (NumberFormatException e) {
                System.out.println("  → Enter a whole number");
            }
        }

        System.out.println("\n  ├─ Medicine Type");
        System.out.println("  │ 1) Oral Medicine");
        System.out.println("  │ 2) Tablet");
        System.out.println("  │ 3) Capsule");
        
        int type = 0;
        while(true) {
            System.out.print("  └─ Choose (1-3): ");
            String typeStr = sc.nextLine();
            try {
                type = Integer.parseInt(typeStr);
                if (type >= 1 && type <= 3) {
                    break;
                } else {
                    System.out.println("  → Choose 1, 2, or 3");
                }
            } catch (NumberFormatException e) {
                System.out.println("  → Enter number 1, 2, or 3");
            }
        }

        Medicine med = switch (type) {
            case 1 -> new OralMedicine(name, dosage, frequency, initialTime, durationDays);
            case 2 -> new Tablet(name, dosage, frequency, initialTime, durationDays);
            case 3 -> new Capsule(name, dosage, frequency, initialTime, durationDays);
            default -> new OralMedicine(name, dosage, frequency, initialTime, durationDays);
        };
        
        med.setNotes(notes);
        return med;
    }
    
    private static int getActiveIndex(Scanner sc, List<Medicine> meds) {
        List<Medicine> activeMeds = new ArrayList<>();
        for(Medicine m : meds) {
            if (m.getIsActive() && !m.getIsCompleted()) {
                activeMeds.add(m);
            }
        }
        
        if (activeMeds.isEmpty()) {
            System.out.println("\n  ✗ No active prescriptions available");
            return -1;
        }

        System.out.println("\n  ╔════ ACTIVE PRESCRIPTIONS ════╗");
        System.out.println("  ║ No │ Name         │ Dosage  │ Status  ║");
        System.out.println("  ╠════════════════════════════════════════╣");
        
        activeMeds.sort(Comparator.comparing(Medicine::getNextTakeTime));
        
        List<Integer> mapToIndex = new ArrayList<>(); 

        for (int i = 0; i < activeMeds.size(); i++) {
            Medicine m = activeMeds.get(i);
            String status = m.getStatusDisplay();
            mapToIndex.add(meds.indexOf(m));

            System.out.printf("  ║ %2d │ %-13s│ %-7s │ %-7s ║\n", (i + 1), m.getName(), m.getDosage(), status);
        }
        System.out.println("  ╚════════════════════════════════════════╝");

        System.out.print("  Select (0 to cancel): ");
        int selection = -1;
        
        String indexStr = sc.nextLine(); 

        try {
            selection = Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            System.out.println("  ✗ Invalid input");
            return -1;
        }
        
        if (selection == 0) {
            return -1;
        }

        if (selection > 0 && selection <= activeMeds.size()) {
            return mapToIndex.get(selection - 1);
        } else {
            System.out.println("  ✗ Invalid selection");
            return -1;
        }
    }
    
    private static void takeAndRecalculate(Scanner sc, List<Medicine> meds) {
        int index = getActiveIndex(sc, meds);
        if (index != -1) {
            Medicine m = meds.get(index);
            
            System.out.println("\n  ╔════ DOSE CONFIRMATION ════╗");
            System.out.println("  ║ " + m.getName() + " (" + m.getDosage() + ")");
            m.take(); 
            
            System.out.print("  Calculate next dose? (y/n): ");
            String confirm = sc.nextLine();
            if (confirm.equalsIgnoreCase("y")) {
                m.calculateNextTime();
            } else {
                System.out.println("  ✓ Time unchanged");
            }
        }
    }

    private static void calculateMedicineUsage(Scanner sc, List<Medicine> meds) {
        if (meds.isEmpty()) {
            System.out.println("\n  ✗ No prescriptions available");
            return;
        }

        System.out.println("\n  ╔════ MEDICINE USAGE CALCULATOR ════╗");
        System.out.println("  ║ Calculates total dosage needed      ║");
        System.out.println("  ╚═══════════════════════════════════╝");

        System.out.println("\n  ├─ SELECT PRESCRIPTION");
        for (int i = 0; i < meds.size(); i++) {
            Medicine m = meds.get(i);
            System.out.printf("  │ %d) %s (%s)\n", (i + 1), m.getName(), m.getDosage());
        }
        
        System.out.print("  └─ Choose (1-" + meds.size() + "): ");
        int choice = -1;
        try {
            choice = Integer.parseInt(sc.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("  ✗ Invalid input");
            return;
        }

        if (choice < 0 || choice >= meds.size()) {
            System.out.println("  ✗ Invalid selection");
            return;
        }

        Medicine med = meds.get(choice);
        long durationDays = med.getTreatmentDurationInDays();

        if (durationDays <= 0) {
            System.out.println("\n  ! This is a continuous prescription (no end date)");
            System.out.print("  How many days to calculate? ");
            try {
                durationDays = Long.parseLong(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Invalid input");
                return;
            }
        }

        String[] freqParts = med.getFrequency().split(" ");
        long frequency;
        String unit;

        try {
            frequency = Long.parseLong(freqParts[0]);
            unit = freqParts[1].toLowerCase().trim();
        } catch (NumberFormatException e) {
            System.out.println("  ✗ Invalid frequency number: " + freqParts[0]);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("  ✗ Invalid frequency format: " + med.getFrequency() + ". Expected 'Number Unit'.");
            return;
        }

        long dosesPerDay = calculateDosesPerDay(frequency, unit);
        
        if (dosesPerDay == 0) {
             System.out.println("  ✗ Cannot calculate: Frequency format requires 'N hours' or 'N days' units.");
             return;
        }
        
        long totalDoses = dosesPerDay * durationDays;

        System.out.println("\n  ╔════ USAGE ANALYSIS ════╗");
        System.out.println("  ║ Medicine: " + String.format("%-24s", med.getName()) + "║");
        System.out.println("  ║ Dosage: " + String.format("%-26s", med.getDosage()) + "║");
        System.out.println("  ║ Frequency: " + String.format("%-23s", med.getFrequency()) + "║");
        System.out.println("  ├────────────────────────┤");
        System.out.println("  ║ Duration: " + String.format("%-24d", durationDays) + "║");
        System.out.println("  ║ Doses/Day: " + String.format("%-23d", dosesPerDay) + "║");
        System.out.println("  ║ TOTAL DOSES: " + String.format("%-21d", totalDoses) + "║");
        System.out.println("  ╚════════════════════════╝");
    }

    private static long calculateDosesPerDay(long frequency, String unit) {
        if (frequency <= 0) return 0;
        
        if (unit.equals("hour") || unit.equals("hours") || unit.equals("hr") || unit.equals("hrs")) {
            return 24 / frequency;
        } else if (unit.equals("day") || unit.equals("days") || unit.equals("d")) {
            return (frequency == 1) ? 1 : 0; 
        } else if (unit.equals("minute") || unit.equals("minutes") || unit.equals("min") || unit.equals("mins")) {
            return (24 * 60) / frequency;
        }
        return 0;
    }
    
    private static void updateMedicineStatus(Scanner sc, List<Medicine> meds) {
        if (meds.isEmpty()) {
             System.out.println("\n  ✗ No prescriptions available");
             return;
        }

        System.out.println("\n  ╔════ STATUS UPDATE ════╗");
        for (int i = 0; i < meds.size(); i++) {
            Medicine m = meds.get(i);
            System.out.printf("  %d) %-20s [%s]\n", (i + 1), m.getName(), m.getStatusDisplay());
        }
        System.out.print("  Select (0 to cancel): ");
        
        int index = -1;
        try {
            index = Integer.parseInt(sc.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("  ✗ Invalid input");
            return;
        }
        
        if (index < 0 || index >= meds.size()) {
            return;
        }
        
        Medicine medToUpdate = meds.get(index);
        
        System.out.println("\n  ╔════ " + medToUpdate.getName().toUpperCase() + " ════╗");
        System.out.println("  ║ Current Status: " + String.format("%-23s", medToUpdate.getStatusDisplay()) + "║");
        System.out.println("  ├────────────────────────┤");
        System.out.println("  ║ 1) Set to ACTIVE       ║");
        System.out.println("  ║ 2) Set to INACTIVE     ║");
        System.out.println("  ║ 3) Set to COMPLETED    ║");
        System.out.println("  ║ 4) View Full Details   ║");
        System.out.println("  ║ 5) Delete Medicine     ║");
        System.out.println("  ║ 6) Cancel              ║");
        System.out.println("  ╚════════════════════════╝");
        
        System.out.print("  Choose (1-6): ");
        int choice = 0;
        try {
            choice = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
            return;
        }

        switch (choice) {
            case 1:
                medToUpdate.setIsActive(true);
                System.out.println("  ✓ Status: ACTIVE");
                break;
            case 2:
                medToUpdate.setIsActive(false);
                System.out.println("  ✓ Status: INACTIVE");
                break;
            case 3:
                System.out.print("  Confirm mark as COMPLETED? (y/n): ");
                if (sc.nextLine().equalsIgnoreCase("y")) {
                    medToUpdate.setIsCompleted(true);
                    System.out.println("  ✓ Status: COMPLETED");
                }
                break;
            case 4:
                medToUpdate.displayInfo();
                break;
            case 5:
                System.out.print("  Confirm DELETE '" + medToUpdate.getName() + "'? (y/n): ");
                if (sc.nextLine().equalsIgnoreCase("y")) {
                    meds.remove(index);
                    System.out.println("  ✓ Medicine deleted successfully");
                }
                break;
            case 6:
                System.out.println("  ✓ Cancelled");
                break;
        }
    }
    
    private static void displayAgeRecommendation(int age) {
        System.out.println("\n  ╔════ DOSAGE GUIDANCE ════╗");
        if (age <= 12) {
            System.out.println("  ║ AGE: Child (0-12)      ║");
            System.out.println("  ║ Use PEDIATRIC dosage   ║");
        } else if (age >= 65) {
            System.out.println("  ║ AGE: Senior (65+)      ║");
            System.out.println("  ║ May need REDUCED dose  ║");
        } else {
            System.out.println("  ║ AGE: Adult (13-64)     ║");
            System.out.println("  ║ Standard dosage OK     ║");
        }
        System.out.println("  ╚════════════════════════╝");
    }

    private static void printMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. Add new prescription");
        System.out.println("2. View dose schedule"); 
        System.out.println("3. Mark dose taken");
        System.out.println("4. Update status");
        System.out.println("5. Calculate medicine usage");
        System.out.println("6. Dosage guidance");
        System.out.println("7. View prescription details");
        System.out.println("8. Search medicine");
        System.out.println("9. Exit");
    }

    private static void viewPrescriptionDetails(Scanner sc, List<Medicine> meds) {
        if (meds.isEmpty()) {
            System.out.println("\n  ✗ No prescriptions available");
            return;
        }

        System.out.println("\n  ╔════ VIEW PRESCRIPTION DETAILS ════╗");
        for (int i = 0; i < meds.size(); i++) {
            Medicine m = meds.get(i);
            System.out.printf("  %d) %s (%s)\n", (i + 1), m.getName(), m.getStatusDisplay());
        }
        System.out.print("  Select (0 to cancel): ");

        int index = -1;
        try {
            index = Integer.parseInt(sc.nextLine()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("  ✗ Invalid input");
            return;
        }

        if (index < 0 || index >= meds.size()) {
            System.out.println("  ✗ Invalid selection");
            return;
        }

        meds.get(index).displayInfo();
    }

    private static void searchMedicine(Scanner sc, List<Medicine> meds) {
        if (meds.isEmpty()) {
            System.out.println("\n  ✗ No prescriptions available");
            return;
        }

        System.out.print("\n  Search medicine name: ");
        String searchTerm = sc.nextLine().toLowerCase().trim();

        System.out.println("\n  ╔════ SEARCH RESULTS ════╗");
        List<Medicine> results = new ArrayList<>();
        for (int i = 0; i < meds.size(); i++) {
            if (meds.get(i).getName().toLowerCase().contains(searchTerm)) {
                results.add(meds.get(i));
                System.out.printf("  %d) %s (%s)\n", (i + 1), meds.get(i).getName(), meds.get(i).getStatusDisplay());
            }
        }

        if (results.isEmpty()) {
            System.out.println("  ✗ No medicines found matching '" + searchTerm + "'");
        }
        System.out.println("  ╚════════════════════════╝");
    }

    private static String nonEmptyLine(Scanner sc) {
        String s;
        do {
            s = sc.nextLine().trim();
            if (s.isEmpty()) System.out.print("  Please enter a value: ");
        } while (s.isEmpty());
        return s;
    }
}

// ============================================================================
// MEDICINE CLASSES (Abstract and Concrete)
// ============================================================================

abstract class Medicine {
    private String name;
    private String dosage;
    private String frequency; 
    private LocalDateTime nextTakeTime;
    private LocalDate startDate;
    private long treatmentDurationInDays; 
    private String notes;
    
    private boolean isCompleted = false;
    private boolean isActive = true;

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Medicine(String name, String dosage, String frequency, LocalDateTime nextTakeTime, long durationDays) {
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.nextTakeTime = nextTakeTime;
        this.startDate = nextTakeTime.toLocalDate(); 
        this.treatmentDurationInDays = durationDays;
        this.notes = "";
        this.isActive = true; 
        this.isCompleted = false;
    }

    public static DateTimeFormatter getDateFormatter() {
        return DATE_FORMATTER;
    }

    public abstract void take();

    public boolean isDurationExceeded() {
        if (treatmentDurationInDays <= 0) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate endDate = startDate.plusDays(treatmentDurationInDays - 1); 
        return today.isAfter(endDate);
    }
    
    public void calculateNextTime() {
        if (isCompleted) {
             System.out.println("\n  ERROR: Treatment is already marked as COMPLETED. Cannot calculate next time.");
             return;
        }
        
        String[] parts = frequency.split(" ");
        if (parts.length != 2) {
            System.out.println("  WARNING: Invalid frequency format. Expected 'Number Unit', e.g., '8 hours'. Cannot calculate next time.");
            return;
        }

        try {
            long value = Long.parseLong(parts[0]);
            String unit = parts[1].toLowerCase().trim();

            Duration duration = switch (unit) {
                case "hours", "hour", "hr", "hrs" -> Duration.ofHours(value);
                case "days", "day", "d" -> Duration.ofDays(value);
                case "minutes", "minute", "min", "mins" -> Duration.ofMinutes(value);
                default -> {
                    System.out.println("  WARNING: Unknown frequency unit '" + unit + "'. Cannot calculate next time.");
                    yield null;
                }
            };

            if (duration != null) {
                this.nextTakeTime = this.nextTakeTime.plus(duration);
                System.out.println("  ✓ Next dose: " + this.nextTakeTime.format(FORMATTER));
                
                if (isDurationExceeded()) {
                    System.out.println("  WARNING: Next dose is past the original " + treatmentDurationInDays + "-day duration.");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("  ERROR: Frequency value '" + parts[0] + "' is not a valid number.");
        }
    }

    public void setIsCompleted(boolean completed) { 
        this.isCompleted = completed; 
        if (completed) {
             this.isActive = false;
        }
    }
    
    public void setIsActive(boolean active) { 
        this.isActive = active; 
        if (active) {
            this.isCompleted = false;
        }
    } 

    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public LocalDateTime getNextTakeTime() { return nextTakeTime; }
    public long getTreatmentDurationInDays() { return treatmentDurationInDays; }
    public boolean getIsCompleted() { return isCompleted; }
    public boolean getIsActive() { return isActive; }
    public LocalDate getStartDate() { return startDate; }
    public String getNotes() { return notes; }
    
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getStatusDisplay() {
        if (isCompleted) return "COMPLETED";
        if (!isActive) return "INACTIVE";
        if (treatmentDurationInDays > 0 && isDurationExceeded()) {
             return "OVERDUE";
        }
        return "ACTIVE";
    }
    
    public void displayInfo() {
        System.out.println("  ┌─────────────────────────────────────┐");
        System.out.println("  │ " + name + " (" + this.toString() + ")");
        System.out.println("  ├─────────────────────────────────────┤");
        System.out.println("  │ Dosage: " + dosage);
        System.out.println("  │ Frequency: " + frequency);
        System.out.println("  │ Next Dose: " + nextTakeTime.format(FORMATTER));
        System.out.println("  │ Status: " + getStatusDisplay());
        
        if (!notes.isEmpty()) {
            System.out.println("  │ Notes: " + notes);
        }
        
        if (treatmentDurationInDays > 0) {
            LocalDate today = LocalDate.now();
            LocalDate endDate = startDate.plusDays(treatmentDurationInDays - 1);
            long daysRemaining = ChronoUnit.DAYS.between(today, endDate) + 1; 
            
            System.out.println("  │ Start: " + startDate.format(getDateFormatter()));
            System.out.println("  │ Duration: " + treatmentDurationInDays + " days");
            
            if (daysRemaining > 0) {
                System.out.println("  │ Days Left: " + daysRemaining);
            }
        } else {
            System.out.println("  │ Duration: Continuous");
        }
        System.out.println("  └─────────────────────────────────────┘");
    }
}

class OralMedicine extends Medicine {
    public OralMedicine(String name, String dosage, String frequency, LocalDateTime nextTakeTime, long durationDays) {
        super(name, dosage, frequency, nextTakeTime, durationDays);
    }
    @Override
    public void take() {
        System.out.println("  → Swallow the Oral Medicine: " + getName());
    }
    @Override
    public String toString() {
        return "Oral Medicine";
    }
}

class Tablet extends Medicine {
    public Tablet(String name, String dosage, String frequency, LocalDateTime nextTakeTime, long durationDays) {
        super(name, dosage, frequency, nextTakeTime, durationDays);
    }
    @Override
    public void take() {
        System.out.println("  → Take Tablet with water: " + getName());
    }
    @Override
    public String toString() {
        return "Tablet";
    }
}

class Capsule extends Medicine {
    public Capsule(String name, String dosage, String frequency, LocalDateTime nextTakeTime, long durationDays) {
        super(name, dosage, frequency, nextTakeTime, durationDays);
    }
    @Override
    public void take() {
        System.out.println("  → Swallow Capsule whole: " + getName());
    }
    @Override
    public String toString() {
        return "Capsule";
    }
}

// ============================================================================
// PATIENT CLASS
// ============================================================================

class Patient {
    private String patientID;
    private String fullName;
    private int age;
    private String gender;
    private LocalDate recordedDate;

    public Patient(String fullName, int age, String gender) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.recordedDate = LocalDate.now();
        this.patientID = "PAT" + System.currentTimeMillis() % 100000;
    }

    public void displayInfo() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║         PATIENT RECORD                 ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║ ID: " + String.format("%-31s", patientID) + "║");
        System.out.println("║ Name: " + String.format("%-29s", fullName) + "║");
        System.out.println("║ Age: " + String.format("%-30d", age) + "║");
        System.out.println("║ Gender: " + String.format("%-27s", gender) + "║");
        System.out.println("║ Recorded: " + String.format("%-26s", recordedDate.toString()) + "║");
        System.out.println("╚════════════════════════════════════════╝");
    }

    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPatientID() { return patientID; }
}