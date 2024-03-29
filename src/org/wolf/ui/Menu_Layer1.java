package org.wolf.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;

import org.wolf.operations.Operations;

public class Menu_Layer1 {

	public static void checkInPatient(BufferedReader reader) throws IOException, SQLException, Exception {
		System.out.println("Please enter | separated patient_id, ward_id, bed_id");
		String input = reader.readLine();
		String[] args = input.split("[|]");
		Integer patientId = Integer.parseInt(args[0]);
		Integer ward_id = Integer.parseInt(args[1]);
		Integer bed_id = Integer.parseInt(args[2]);
		if(Operations.checkInPatient(patientId, ward_id, bed_id)){
			System.out.println("Operation Successful");
		} else{
			System.out.println("Operation Failed");
		}
	}

	public static void checkOutPatient(BufferedReader reader) throws IOException, SQLException, Exception {
		System.out.println("Please enter checkIn-Id");
		String input = reader.readLine();
		Integer checkInId = Integer.parseInt(input);
		if(Operations.checkoutPatient(checkInId)){
			System.out.println("Operation Successful");
		} else{
			System.out.println("Operation Failed");
		}
	}

	public static void undergoTests(BufferedReader reader) throws IOException, SQLException {
		System.out.println("Please | space separated recordId, testId, test_date and result");
		String input = reader.readLine();
		String[] args = input.split("[|]");
		Integer recordId = Integer.parseInt(args[0]);
		Integer testId = Integer.parseInt(args[1]);
		String test_date = args[2];
		String result = args[3];
		if(Operations.addTestToTreatment(recordId, testId, test_date, result)){
			System.out.println("Operation Successful");
		} else{
			System.out.println("Operation Failed");
		}
	}

	public static void createTreatment(BufferedReader reader) throws IOException, SQLException{
		System.out.println("Enter | separated patient_id, diagnosis, prescription, responsible_doctor, treatment_plan, reg_fee, medical_fee");
		String input = reader.readLine();
		String[] args = input.split("[|]");
		Integer patient_id = Integer.parseInt(args[0]);
		String diagnosis = args[1]; 
		String prescription = args[2]; 
		Integer responsible_doctor = Integer.parseInt(args[3]);
		Integer process_treatment_plan = Integer.parseInt(args[4]);
		Double reg_fee = Double.parseDouble(args[5]); 
		Double medical_fee = Double.parseDouble(args[6]);
		if(Operations.createTreatment(patient_id, diagnosis, prescription, responsible_doctor,process_treatment_plan, reg_fee, medical_fee)){
			System.out.println("Operation Successful");
		} else{
			System.out.println("Operation Failed");
		}
		
	}

	public static void endTreatment(BufferedReader reader) throws IOException, SQLException{
		System.out.println("Enter | separated med.recordId and treatment fee");
		String input = reader.readLine();
		String[] args = input.split("[|]");
		Integer record_id = Integer.parseInt(args[0]);
		Double treatment_fee = Double.parseDouble(args[1]);
		if(Operations.endTreatment(record_id, treatment_fee)){
			System.out.println("Operation Successful");
		} else{
			System.out.println("Operation Failed");
		}
	}
	public static void pay(BufferedReader reader) throws IOException {
		System.out.println("Enter | separated billId, payer_ssn, bill_address, amountPaying");
		String input = reader.readLine();
		String[] args = input.split("[|]");
		Integer billId = Integer.parseInt(args[0]); 
		String payer_ssn = args[1]; 
		String bill_address = args[2];
		Double amountPaid = Double.parseDouble(args[3]); 
		System.out.println("Choose payment method");
		System.out.println("1.Cash");
		System.out.println("2.Card");
		System.out.println("3.Insurance");
		System.out.println("4.Cancel Payment");
		input = reader.readLine();
		switch(input){
		case "1": if(Operations.payCash(billId, payer_ssn, bill_address, amountPaid)){
			System.out.println("Operation Successful");
		} else{
			System.out.println("Operation Failed");
		}
			break;
		case "2": 
			System.out.println("Enter card number");
			String card_no = reader.readLine();
			if(Operations.payCard(billId, payer_ssn, bill_address, amountPaid,card_no)){
				System.out.println("Operation Successful");
			} else{
				System.out.println("Operation Failed");
			}
			break;
		case "3":
			System.out.println("Enter insurance policy number");
			String policy_no = reader.readLine();
			if(Operations.payInsurance(billId, payer_ssn, bill_address, amountPaid, policy_no)){
				System.out.println("Operation Successful");
			} else{
				System.out.println("Operation Failed");
			}
			break;
		case "4":return;
		default: System.out.println("Enter a valid choice");
		}
	}

}
