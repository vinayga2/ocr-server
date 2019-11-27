package dynamic.groovy.mbm

class TextForHighlight {
    String companyCode;
    String filename;
    List<String> highlight = new ArrayList<>();

    final static String KRS = """
        Laboratory, Patient, Name, Renal, Vital Signs, Assessment, Allergies, 
        Creatinine Ratio, Creatinine, BUN, URR, Kt/v, Kt/v PD, spKt/v, Potassium, Carbon Dioxide, Bicarbonate, Calcium, Phosphorous, 
        Ca, Phos Product, Albumin, Iron(Fe), Iron, Transferrin Saturation, Pth intact, Cholesterol, Triglycerides, 
        HDL, LDL, Hgb A1c, Hemoglobin,        
        Creatinine, BUN, Urine Protein, Creatinine Ratio, HR Urine Protein, 
        CrCL HR Urine Chol., Triglyceride, HDL, LDL, Potassium, CO2, Bicarbonate, Albumin, Iron, TSAT, Ca, Ph, Pth intact, 
        HGBa1c, African American, Non-African American, Phosphorus, Sodium, Magnesium, Glucose
        Weight, Dry weight, Average IDWG, Height, Blood pressure, BP, Dialysis Modality, Primary Access ,
        Aranesp, Epogen, Epoetin, Hectorol, Doxercalciterol, Zemplar, Paricalcitol, Venofer, Iron Sucrose, 
        Vancomycin, Infed, Iron, Liquacel, Nephrocarb, Mircera, Protein Bar, Gentamycin    
    """;
    final static String OHUM = """
        Accident Ambulance Appeals ASAM Aurora Autism Spectrum Disorder Behavioral Health Benefit BIPAP
        Briova BriovaRx Chemotherapy Clinical Criteria Clinical Trial Cochlear Implant Complaint Cosmetics
        CPAP CT Scan Dental Diagnostics Dialysis Discharge plan DME Doctor ECG Echocardiogram
        EKG Elective Emergency Exclusion Facility Gender Reassignment Genetic Testing HIPAA History Home Infusion Hospice Hospital
        Imaging Injectables Inpatient Labs Limitation Long Term Acute Care LTAC LVAD Magnetic Resonance Angiography Magnetic Resonance Imaging 
        Maternity MCG Medications Mental Health MRA MRI Neurologic Monitoring Non-Emergency Observation OON OptumRx Oral Appliance
        Oral Surgery Orthotics Out Network Outpatient Oxygen Concentrator Past Medical History Peer PET Scan Pharmacy Positron Emission Tomography
        Prosthetics Provider Radiation Radiation Therapy Reconstructive Referral Rehab Rehabilitation
        Residential Skilled Nursing Facility SNF Substance Abuse Surgery Surgical Temporomandibular Joint Dysfunction
        TMJ Transplant Transportation Treatment Urgent VAD Work Related Injury Worker's Compensation
    """;
    final static String NRS = """
        Abuse Neglect Acute inpatient rehabilitation Acute Respiratory Failure Admission AIR AIR Analgesic Antibiotic Antidepressant Withdrawal
        Antireflux Anuria Apnea Apnea Prematurity Appeal Assessment Baby Bacteria BCRT Bed Level Behevioral Benzodiasepines
        Bereavement Bilirubin Blood Culture bottle BPD Bradycardia Bradycardia Breastfeed
        Bronchopulmonary Dysplasia C&S Caffeine Car seat Cardiomyopathy Cardiorespiratory Caregiver Case Management
        Cases CDH Chest X-ray Chronic Care Chronic Lung Disease Circumcision CLD Clonidin CNS Collar Community State
        Complex Concurrent Congenital Diaphragmatic Hernia Congenital Heart Disease CPAP CPR Crib CSF Cystic Fibrosis
        Denial Desaturation Detained Baby Detoxification Diagnostic Discharge Plan DME Documentation Donor Milk
        DRG Drug Screening ELBW Encephalopathy Enteral Feedings Evaluation Exchange Infusion Facility Fax Federal Mandate
        Feeding Feeding Feeding intolerance Feeds Finnegan Scoring Fluid Resuscitation Formula Gastric Tube Gastroesophageal Reflux
        Gastrostomy Gastrostomy tube GBS Gestational Age Hearing Screening HEDIS hemodynamic Hemolysis Home monitor
        Hospital Day Hyperbilirubinemia Hyperthermia Hypoglycemia Hypoglycemia Hypothermia Hypoxemia Imaging Immunization Inappropriate Referral
        Incubation Incubator Infant Initial Isolette IV blood pressure Laboratory Last cover date Level Level Care Leveling Long Term Acute Care
        Low birth weight LTAC Lumbar Puncture Management Medical Medical Director Medical History Medication Milk Monitoring Morphine MRI NAS NEC Neonatal Abstinence Syndrome
        Neonatal Intensive Care Unit Neonatal Opioid Withdrawal Syndrome Neonatal Resource Services Neonatal Sepsis Neonate Network Gap
        Newborn Screening Next review date Nicotine Withdrawal NICU Nipple Feeding Nitric Oxide NNS Nonnutritive sucking
        NOWS NRS Nutrition Occupational Therapy Ophthalmology Oral Feeding Oxygen Oxygen Saturation PAC Palivizumab PCGs Pedia Peds Persistent Pulmonary Hypertension
        Pharmacology Pharmacy Phenobarbital Phototherapy Physician Pneumocardiograms PPHN Premature Private Duty Nursing Probiotics Provider
        Pulmonary Hypoplasia Radiant Warmer Readmission Referral Respiratory Respiratory Syncytial Virus Respiratory Syncytial Virus Retrospective Revenue Code
        ROP RSV Seizure Sepsis Sepsis Severe Anemia Short Stay SIDS Skilled Nursing Facility SNF SNF Speech Therapy Stool Testing Strategy Stratification Suck
        Supplementation Surfactant Surgical Swallow Symptoms Synagis Tachycardia Tachypnea Telephonic Temperature Theophylline Therapy Thermoregulation Total Parental Nutrition
        TPN Tracheostomy Transfer Transfusion Transition Treatment Tube Feedings UAC Utilization Management UVC Ventilation Ventilator VLBW Weaning Weight Weight Gain Weight Loss
    """;

    TextForHighlight(String companyCode, String faxFile) {
        this.companyCode = companyCode;
        filename = faxFile;
        setupHighligher();
    }

    List<String> getHighlight() {
        return highlight
    }

    void setupHighligher() {
        String mergeText = null;
        if (companyCode.equalsIgnoreCase("OHUM")) {
            mergeText = OHUM;
        }
        else if (companyCode.equalsIgnoreCase("KRS")) {
            mergeText = KRS;
        }
        else if (companyCode.equalsIgnoreCase("NRS")) {
            mergeText = NRS;
        }
        String[] strArr = mergeText.split("[,\\s]");
        for (String str:strArr) {
            String tmp = str.trim();
            if (tmp != null && !tmp.isEmpty()) {
                if (!highlight.contains(tmp.toUpperCase())) {
                    highlight.add(tmp.toUpperCase());
                }
            }
        }
        Collections.sort(highlight);
    }

    boolean existInHighlight(String str, int pageNum) {
        boolean b = false;
        String existStr = null;
        for (String high:highlight) {
            if (str.equalsIgnoreCase(high)) {
                b = true;
                existStr = high;
                break;
            }
            else {
                if (str.toUpperCase().contains(high+".") || str.toUpperCase().contains(high+",") || str.toUpperCase().contains(high+"/") || str.toUpperCase().contains(high+"-") || str.toUpperCase().contains(high+":")) {
                    b = true;
                    existStr = high;
                    break;
                }
            }
        }
        return b;
    }

}
