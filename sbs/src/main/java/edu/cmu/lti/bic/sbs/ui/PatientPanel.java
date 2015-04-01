package edu.cmu.lti.bic.sbs.ui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class PatientPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8006533646058966961L;
	JLabel basicLabel;
	JLabel descriptionLabel;

	String basic = "basic";
	String description = "description";
	/**
	 * constructor initialize the patient panel
	 */
	public PatientPanel() {
		//integrate this title border
		TitledBorder tb=new TitledBorder(null, "Patient", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(255,255,255));
		this.setBorder(tb);
		
		basicLabel = new JLabel();
		descriptionLabel = new JLabel();
		this.add(basicLabel);
		this.add(descriptionLabel);
		this.setOpaque(false);
	}
	/**
	 * setDescription sets up the description of patient
	 * @param str
	 */
	void setDescription(String str) {
		descriptionLabel.setForeground(new Color(255, 255, 255));
		descriptionLabel.setText(String.format("<html><div style=\"width:%dpx;\">%s</div><html>", 50, str));
	}
	/**
	 * setBasic sets up the basic information about the patient
	 * @param str
	 */
	void setBasic(String str) {
		basicLabel.setForeground(new Color(255, 255, 255));
		basicLabel.setText(String.format("<html><div style=\"width:%dpx;\">%s</div><html>", 50, str));
	}
}
