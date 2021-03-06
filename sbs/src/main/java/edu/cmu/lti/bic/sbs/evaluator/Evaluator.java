package edu.cmu.lti.bic.sbs.evaluator;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import edu.cmu.lti.bic.sbs.engine.Engine;
import edu.cmu.lti.bic.sbs.gson.Drug;
import edu.cmu.lti.bic.sbs.gson.Patient;
import edu.cmu.lti.bic.sbs.gson.Prescription;
import edu.cmu.lti.bic.sbs.gson.Tool;
import edu.cmu.lti.bic.sbs.simulator.MedicalParameter;

// Test class
class BloodPressure implements MedicalParameter {

}

/**
 * 
 * @author victorzhao, xings, ryan sun
 *
 */
public class Evaluator {

  private double score;

  private Path actual;

  private Path goldStandard;

  private Step currentStep;

  private ScoreDP scoreDP;

  private long baseTimeInMills = 0;
  
  private String userName;
  
  // private String report;
  public Evaluator(Engine engine) {
    this.engine = engine;
    actual = new Path();
    currentStep = new Step();
    actual.setTag("Actual");
    initGoden();
    scoreDP = new ScoreDP();
    baseTimeInMills = Calendar.getInstance().getTimeInMillis();
  }

  /**
   * Initialize Golden Standard Path
   * 
   */

  private void initGoden() {
    goldStandard = new Path();
    goldStandard.setTag("Gold Standard");
    goldStandard.add(new Step(new Patient(), new Prescription(), new Tool("codeblue", "Call Code",
            0), (int)Calendar.getInstance().getTimeInMillis()));
    goldStandard.add(new Step(new Patient(), new Prescription(), new Tool("OxygenMask",
            "Face Mask", 100), (int)Calendar.getInstance().getTimeInMillis()));
    goldStandard
            .add(new Step(new Patient(), new Prescription(new Drug("naloxone", "Naloxone", "naloxone"),
                    200.0, "mcg"), new Tool(), (int)Calendar.getInstance().getTimeInMillis()));
  }

  class Report {
    double score;

    String report;
  }

  /**
   * called by engine to receive the medPara
   * 
   * @param medPara
   *          , MedicalParameter is an interface in simulator package
   */
  public void receivePara(MedicalParameter medPara) {
    System.out.println("evaluator.ReceivePara called by engine!");
  }

  private Engine engine;

  // private String report;

  // overloading the constructor to support initialize with engine parameter

  public void receive(Patient patient, Calendar time) {
    currentStep.setPatient(patient);
    int curTime = (int)(Calendar.getInstance().getTimeInMillis() - baseTimeInMills);
    System.out.print(curTime);
    currentStep.setTime(curTime);
    System.out.println("Patient added");
    updateStep();
  }

  public void receive(Prescription prescription, Calendar time) {
    currentStep.setPrescription(prescription);
    int curTime = (int)(Calendar.getInstance().getTimeInMillis() - baseTimeInMills);
    currentStep.setTime(curTime);
    System.out.println("Evaluator: USER ACTION: USE DRUG:" + prescription.getDrug().getName());
    updateStep();
  }
  
  public void receive(String name){
    this.userName = name;
  }

  /**
   * called by engine to receive the Equipment variables
   * 
   * @param tool
   *          Equipment is a Class defined in gson package
   * @param time
   *          time used
   */

  public void receive(Tool tool, Calendar time) {
    currentStep.setTool(tool);
    int curTime = (int)(Calendar.getInstance().getTimeInMillis() - baseTimeInMills);
    currentStep.setTime(curTime);
    System.out.println("Evaluator: USER ACTION: USE TOOL:" + tool.getName());
    updateStep();
  }

  public void receive(Calendar time) {
    int curTime = (int)(Calendar.getInstance().getTimeInMillis() - baseTimeInMills);
    currentStep.setTime(curTime);
    updateStep();

  }

  public void regularUpdate(Patient p, Calendar time) {
    currentStep.setPatient(p);
    int curTime = (int)(Calendar.getInstance().getTimeInMillis() - baseTimeInMills);
    currentStep.setTime(curTime);
    if (isSimEnd()) {
      calculateScore();
      engine.simOver(score, generateReport());
    }
  }

  public State lastHealthyState(){
    int i = actual.size() - 1;
    while(actual.get(i).getPatient().isConditionBad()){
      i--;
      if (i < 0) return null;
    }
    Step lastHealthy = actual.get(i);
    State result = new State();
    result.setPatient(lastHealthy.getPatient());
    while(i >= 0){
      result.getPrescriptions().add(actual.get(i).getPrescription());
      result.getTools().add(actual.get(i).getTool());
    }
    return result;
  }
  
  public boolean isSimEnd() {
    if (actual.size() == 0) return false;
     int timeNow = currentStep.getTime();
     int timeLast = actual.get(actual.size() - 1).getTime();
     Patient p = currentStep.getPatient();
     return 60000 < timeNow-timeLast && (p.isConditionStable() || p.isConditionBad());
  }

  /**
   * called by engine to receive the Equipment variables
   *
   * @param tool
   *          Equipment is a Class defined in gson package
   * @param time
   *          time used
   */

  public void calculateScore() {
    scoreDP.scoreDP(goldStandard, actual);
    score = this.getPatientScore();
  }

  public void calculateScorePending() {
    scoreDP.scoreDPpending(goldStandard, actual);
    score = this.getPatientScore();
  }

  public double getScore() {
    return score;
  }
  
  public double getPatientScore(){
    return actual.patientScore();
  }

  public void updateStep() {
    if (currentStep.isComplete()) {
      actual.add(currentStep);
      currentStep = new Step();
    }
  }

  public String toString() {
    return "The score is " + score;
  }

  public void setInitialTime(Calendar initTime) {

  }

  private String generateReport() {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter("src/test/resources/report.json", "UTF-8");
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Gson gson = new Gson();
    Report r = new Report();
    r.report = this.toString();
    r.score = this.score;
    String report = gson.toJson(r);
    writer.println(report);
    writer.close();

    // Add the traceback information
    StringBuilder sb = new StringBuilder(report);
    sb.append("\n");
    sb.append("Patient score is: ");
    sb.append(getPatientScore());
    sb.append("\n");
    sb.append("The user's correct actions are :" + "\n");

    for (Step s : scoreDP.getBacktrack()) {
      sb.append(s.getStep(true));
      // sb.append("\n");
    }
    System.out.println(sb.toString());
    
    
    // generate the txt report
    String reportTxt = txtReportGenerator(score);
    
    
    // TODO: Set the patient score.
    // Where can I set the patie<nt score??
    return reportTxt;
  }

  public static Path loadGS(String filepath) throws Exception{
      String str;
      try {
	  File file = new File(filepath);
	  FileInputStream fis = new FileInputStream(file);
	  byte[] data = new byte[(int) file.length()];
	  fis.read(data);
	  fis.close();
	  str = new String(data, "UTF-8");
      } catch (Exception e) {
	  throw e;
      }      
      
      Gson gson = new Gson();
      Path gs;
      try {
	  gs = gson.fromJson(str, Path.class);
      } catch (JsonSyntaxException e) {
	  throw new Exception(e);
      }
      return gs;
  }

  private String txtReportGenerator(double score){
    String outputFile = "Report.txt";
    StringBuilder output = new StringBuilder();
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false));
      
      output.append("Here is the report for ");
      output.append(userName + ":" + "\n");
      output.append("The final score " + userName + " get is : " 
                      + String.format("%.2f\n", score));
      
//      output.append("The helpful steps and details "  
//                      + userName + " did is listed below : \n");
//      
//      output.append("Time\t Drug Used\t\t Drug Dose\t Drug Unit\t\t    Action\n");
//      
//      for (Step s : scoreDP.getBacktrack()) {
//        output.append(s.getStep(true));
//      }
//      
      
      output.append("\nThe actual steps and details "  
              + userName + " did are listed below : \n");

      output.append("Time\t\tDrug\t\tDose\t\tAction\n");
      
      for (Step s : actual) {
        output.append(s.getStep(true));
      }
      
      output.append("\n The suggested actions are listed below : \n");
      
      output.append("Drug\t\tDose\t\tAction\n");
      
      for (Step s : goldStandard) {
        output.append(s.getStep(false));
      }
      
      // Print the patient state
      if (actual.getBpHighTime()>0)
        output.append("The blood pressure is too high for " +actual.getBpHighTime()+ " seconds\n");
      if (actual.getBpLowTime()>0)
        output.append("The blood pressure is too low for " +actual.getBpLowTime()+ " seconds\n");
      if(actual.getHrHighTime()>0)
        output.append("The heart rate is too high for " +actual.getHrHighTime()+ " seconds\n");
      if(actual.getHrLowTime()>0)
        output.append("The heart rate is too low for " +actual.getHrLowTime()+ " seconds\n");
      if(actual.getOlTime()>0)
        output.append("The oxygen level is too low for " +actual.getOlTime()+ " seconds\n");
      if(actual.getRrHighTime()>0)
        output.append("The respiratory rate is too high for " +actual.getRrHighTime()+ " seconds\n");
      if(actual.getRrLowTime()>0)
        output.append("The respiratory rate is too low for " +actual.getRrLowTime()+ " seconds\n");

      
      bw.write(output.toString());
      System.out.println(output);
      
      bw.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return output.toString();
  }
  
  // Main method for testing
  public static void main(String[] args) {
      // Test for generate the path gson for Xing Sun to write the json file. 
      Gson gson = new Gson();
      ArrayList<Step> a = new ArrayList<Step>();
      a.add(new Step(new Patient(), new Prescription(), new Tool("codeblue", "Call Code",
	            0), (int)Calendar.getInstance().getTimeInMillis()));
      a.add(new Step(new Patient(), new Prescription(), new Tool("codeblue", "Call Code",
	            0), (int)Calendar.getInstance().getTimeInMillis()));
      a.add(new Step(new Patient(), new Prescription(), new Tool("codeblue", "Call Code",
	            0), (int)Calendar.getInstance().getTimeInMillis()));
      System.out.println(gson.toJson(a));
  }
}
