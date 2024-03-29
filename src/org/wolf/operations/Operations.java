package org.wolf.operations;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.wolf.baseclasses.BillingAccount;
import org.wolf.baseclasses.BillingAccountView;
import org.wolf.baseclasses.MedicalRecord;
import org.wolf.baseclasses.Payments;
import org.wolf.baseclasses.Test;
import org.wolf.baseclasses.Ward_Patient;
import org.wolf.config.DatabaseConnection;
import org.wolf.crud.BillingAccountCRUD;
import org.wolf.crud.MedicalRecordCRUD;
import org.wolf.crud.PaymentsCRUD;
import org.wolf.crud.TestCRUD;
import org.wolf.crud.Test_MedicalRecordsCRUD;
import org.wolf.crud.WardCRUD;
import org.wolf.crud.Ward_PatientCRUD;

public class Operations {

	/**
	 * Checks if you have a medical record whose end date i s null, if not then create a medical record before checking in
	 * Assuming the bed availability has been checked
	 * Adds the patient info in ward_patient checkin table - to signify that the patient has checked in
	 * Update the availability of the Bed, since he/she have occupied them
	 * @param patientId
	 * @param ward_id
	 * @param bed_id
	 * @returnReturn true if successful else false
	 * @throws SQLException
	 * @throws Exception
	 */
	public static Boolean checkInPatient(Integer patientId, Integer ward_id,Integer bed_id) throws SQLException,Exception{
		Connection conn = null;
		try {
			// Get database connection
			conn = DatabaseConnection.getConnection();
			
			// set autoCommit to false to make sure no commit happens in case of SQLException
			conn.setAutoCommit(false);
			
			ArrayList<MedicalRecord> l = MedicalRecordCRUD.getMedicalRecordsForPatientEndDateNull(patientId);
			//No active medical record
			if(l.size()==0){
				return false;
			}
			
			String end_time = "";
			Integer checkinId = Ward_PatientCRUD.insertWardPatient(patientId, end_time, ward_id, bed_id);
			if(checkinId == null){
				conn.rollback();
				return false;
			}
			WardCRUD.occupyBed(ward_id, bed_id);
			System.out.println("The new checkin Id is "+ checkinId);
			
			// If successfully performed the operation, commit the changes in the database.
			conn.commit();
			
			return true;
			
		} catch(SQLException ex) {
			// In case of any SQLException print the stack trace.
			ex.printStackTrace();
			if(conn != null) {
				try {
					// In case of any other SQLException than connection establishment, rollback the transaction.
	                System.err.print("Transaction is being rolled back");
	                conn.rollback();
	            } catch(SQLException excep) {
	                excep.printStackTrace();
	            }
			}
			return false;
		} catch(Exception excp) {
			// In case of any other Exception print the stack trace.
			excp.printStackTrace();
			if(conn != null) {
				try {
					// In case of any Exception than connection establishment, rollback the transaction.
	                System.err.print("Transaction is being rolled back");
	                conn.rollback();
	            } catch(SQLException excep) {
	                excep.printStackTrace();
	            }
			}
			return false;
		} finally {
			//Finally set the auto commit to true
			conn.setAutoCommit(true);
		}
	}
	
	/**
	 * Checks out a patient from a ward by  performing the following
	 * Update the record in the Ward_patient Checks in table, by setting its end tiem to the current timestamp
	 * Releases the bed, which was occupied by the patient
	 * Gets the latest unpaid bill for the patient and adds the accommodation fee to the bill based on the time he was checked in
	 * @param checkinId
	 * @return
	 * @throws SQLException
	 * @throws Exception
	 */
	public static Boolean checkoutPatient(Integer checkinId) throws SQLException, Exception{
		Connection conn = null;
		try {
			// Get database connection
			conn = DatabaseConnection.getConnection();
			
			// set autoCommit to false to make sure no commit happens in case of SQLException
			conn.setAutoCommit(false);
			
			//Get the checkInRecord
			Ward_Patient w = Ward_PatientCRUD.viewWardPatients(checkinId);
			//Update the checkoutTime in the record
			Ward_PatientCRUD.updateWardEndtime(checkinId);
			//Release the occupied Bed
			WardCRUD.releaseBed(w.ward_id, w.bed_id);
			//Get the latest unpaid bill for the patientId.
			BillingAccount bill = BillingAccountCRUD.internalGetLatestUnpaidBill(w.patient_id);
			if(bill == null){
				System.out.println("No Unpaid Bill available generate a new med record and bill");
				conn.rollback();
				return false;
			}
			//Calculate the accommodation Fee for the time stayed
			Double accomFee = WardCRUD.calculateAccomCharges(checkinId);
			//Add it the calculated fee to the original
			Double b = (bill.accom_fee==null)?accomFee:(bill.accom_fee+accomFee);
			//Update the bill with this fee
			BillingAccountCRUD.updateBillingAccount(bill.bill_id, bill.patient_id, bill.record_id, bill.payment_status, bill.reg_fee, b,bill.medical_fee);
			//If everything goes okay, commit the changes
			conn.commit();
			return true;
			
		} catch(SQLException ex) {
			
			// In case of any SQLException print the stack trace.
			ex.printStackTrace();
			
			if(conn != null) {
				try {
					// In case of any other SQLException than connection establishment, rollback the transaction.
	                System.err.print("Transaction is being rolled back");
	                conn.rollback();
	            } catch(SQLException excep) {
	                excep.printStackTrace();
	            }
			}
			return false;
		} catch(Exception excp) {
			// In case of any other Exception print the stack trace.
			excp.printStackTrace();
			if(conn != null) {
				try {
					// In case of any Exception than connection establishment, rollback the transaction.
	                System.err.print("Transaction is being rolled back");
	                conn.rollback();
	            } catch(SQLException excep) {
	                excep.printStackTrace();
	            }
			}
			return false;
		} finally {
			//Finally set the auto commit to true
			conn.setAutoCommit(true);
		}
	}
	
	/**
	 * This function creates a medical record and also creates a corresponding bill for it
	 * @param patient_id - The patient should exist from before
	 * @param diagnosis
	 * @param prescription
	 * @param responsible_doctor
	 * @param process_treatment_plan
	 * @param reg_fee - initial registration fee
	 * @param medical_fee - initial medical  fee
	 * @return true when treatment is created successfully else false
	 * @throws SQLException 
	 */
	public static Boolean createTreatment(Integer patient_id, String diagnosis, String prescription, Integer responsible_doctor, Integer process_treatment_plan, Double reg_fee, Double medical_fee) throws SQLException{
		Connection conn = null;
		try {
			
		// Get database connection
		conn = DatabaseConnection.getConnection();
						
		// set autoCommit to false to make sure no commit happens in case of SQLException
		conn.setAutoCommit(false);
		
		String endDate = null;
		String payment_status = "N";
		Double accom_fee = 0.0;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDate = LocalDateTime.now();
		String start_date = dtf.format(localDate);
		Integer record_id = MedicalRecordCRUD.insertMedicalRecord(patient_id, start_date, endDate, diagnosis, prescription, responsible_doctor,process_treatment_plan);
		if(record_id == null){
			System.out.println("Couldnt create medical record");
			return false;
		}
		BillingAccountCRUD.insertBillingAccount(patient_id, record_id, payment_status, reg_fee, accom_fee, medical_fee);
		conn.commit();
		return true;
		}
		 catch(SQLException ex) {
				
				// In case of any SQLException print the stack trace.
				ex.printStackTrace();
				
				if(conn != null) {
					try {
						// In case of any other SQLException than connection establishment, rollback the transaction.
		                System.err.print("Transaction is being rolled back");
		                conn.rollback();
		            } catch(SQLException excep) {
		                excep.printStackTrace();
		            }
				}
				return false;
			} finally {
				//Finally set the auto commit to true
				conn.setAutoCommit(true);
			}
	}
	
	public static Boolean addTestToTreatment(Integer recordId, Integer testId, String test_date, String result) throws SQLException{
		Test_MedicalRecordsCRUD.insertTest_MedicalRecords(recordId, testId, test_date, result);
		Test t = TestCRUD.viewTest(testId);
		BillingAccountCRUD.updateMedicalFee(recordId, t.fees);
		return true;
	}
	
	public static Boolean endTreatment(Integer recordId, Double treatmentFee) throws SQLException{
		Connection conn = null;
		try {
			
		// Get database connection
		conn = DatabaseConnection.getConnection();
						
		// set autoCommit to false to make sure no commit happens in case of SQLException
		conn.setAutoCommit(false);
		
		//Add the treatment fee to the existing medical fee
		BillingAccountCRUD.updateMedicalFee(recordId, treatmentFee);
		MedicalRecordCRUD.updateMedicalRecordEndTime(recordId);
		conn.commit();
		return true;
		} catch(SQLException ex) {
			
			// In case of any SQLException print the stack trace.
			ex.printStackTrace();
			
			if(conn != null) {
				try {
					// In case of any other SQLException than connection establishment, rollback the transaction.
	                System.err.print("Transaction is being rolled back");
	                conn.rollback();
	            } catch(SQLException excep) {
	                excep.printStackTrace();
	            }
			}
			return false;
		} finally {
			//Finally set the auto commit to true
			conn.setAutoCommit(true);
		}
	}
	
	public static Boolean payCash(Integer billId, String payer_ssn, String bill_address, Double amountPaid) {
		try {
		
			BillingAccountView bill = BillingAccountCRUD.viewBillingAccountsByBill(billId);
			PaymentsCRUD.insertPayment(billId, payer_ssn, bill_address, "", "", amountPaid);
			
			ArrayList<Payments> p = PaymentsCRUD.getPaymentsForBill(billId);
			Double paid = 0.0;
			
			for (Payments payment:p){
				paid += payment.amountPaid;
			}
				
			Double amountDue = (bill.accom_fee+bill.reg_fee+bill.medical_fee - paid);
			
			//Bill completely Paid
			if(amountDue <= 0){
				BillingAccountCRUD.updateBillingAccount(billId, bill.patient_id, bill.record_id, "Y", bill.reg_fee, bill.accom_fee, bill.medical_fee);
			}
			
			return true;
			
		} catch (SQLException ex) {
	    	System.err.println(ex.getMessage());
	    	return false;
	    }
	}
	
	public static Boolean payInsurance(Integer billId, String payer_ssn, String bill_address, Double amountPaid, String policy_no) {
		try {
			BillingAccountView bill = BillingAccountCRUD.viewBillingAccountsByBill(billId);
			PaymentsCRUD.insertPayment(billId, payer_ssn, bill_address, policy_no, "", amountPaid);
			
			ArrayList<Payments> p = PaymentsCRUD.getPaymentsForBill(billId);
			Double paid = 0.0;
			
			for (Payments payment:p){
				paid += payment.amountPaid;
			}
				
			Double amountDue = (bill.accom_fee+bill.reg_fee+bill.medical_fee - paid);
			
			//Bill completely Paid
			if(amountDue <= 0){
				BillingAccountCRUD.updateBillingAccount(billId, bill.patient_id, bill.record_id, "Y", bill.reg_fee, bill.accom_fee, bill.medical_fee);
			}
			
			return true;
			
		} catch (SQLException ex) {
	    	System.err.println(ex.getMessage());
	    	return false;
	    }
	}
	
	public static Boolean payCard(Integer billId, String payer_ssn, String bill_address, Double amountPaid, String card_no) {
		try {
			BillingAccountView bill = BillingAccountCRUD.viewBillingAccountsByBill(billId);
			PaymentsCRUD.insertPayment(billId, payer_ssn, bill_address, "",card_no, amountPaid);
			
			ArrayList<Payments> p = PaymentsCRUD.getPaymentsForBill(billId);
			Double paid = 0.0;
			
			for (Payments payment:p){
				paid += payment.amountPaid;
			}
			
			Double amountDue = (bill.accom_fee+bill.reg_fee+bill.medical_fee - paid);
			
			//Bill completely Paid
			if(amountDue <= 0){
				BillingAccountCRUD.updateBillingAccount(billId, bill.patient_id, bill.record_id, "Y", bill.reg_fee, bill.accom_fee, bill.medical_fee);
			}
			
			return true;
			
		} catch (SQLException ex) {
	    	System.err.println(ex.getMessage());
	    	return false;
	    }
	}
}
