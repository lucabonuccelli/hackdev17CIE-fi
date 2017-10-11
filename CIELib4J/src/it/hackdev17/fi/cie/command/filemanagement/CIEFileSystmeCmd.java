package it.hackdev17.fi.cie.command.filemanagement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.xml.bind.DatatypeConverter;

import it.hackdev17.fi.cie.exception.BuildCommandException;
import it.hackdev17.fi.utils.ResourceHandler;

/**
 * Helper class for accessing CIE file system DF and EF
 * @author HoochDeveloper (aka Michele Salvatore Rillo)
 * 2017
 */
public class CIEFileSystmeCmd {

	public static final byte[] CIE_AID = { (byte) 0xA0, 0x00, 0x00, 0x00, 0x00, 0x39 };

	public static final byte[] AIS_AID = { (byte) 0xA0, 0x00, 0x00, 0x00, 0x30, (byte) 0x80, 0x00, 0x00, 0x00, 0x09,
			(byte) 0x81, 0x60, 0x01 };

	private final boolean isDebugEnabled;
	
	private final CardChannel ch;

	public CIEFileSystmeCmd(CardChannel ch, boolean debug) {
		this.ch = ch;
		this.isDebugEnabled = debug;
	}

	/**
	 * Select the AIS applet starting from root, current file system selection is lost
	 * @throws BuildCommandException
	 * @throws CardException
	 */
	public void selectAISApplet() throws BuildCommandException, CardException {
		APDUFileSystemCmd.selectRoot();
		this._selectAISApplet();
	}

	/**
	 * Select AIS applet from current file system selection
	 * @throws BuildCommandException
	 * @throws CardException
	 */
	private void _selectAISApplet() throws BuildCommandException, CardException {
		CommandAPDU selByAIDcmd = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte AID_LENGTH = (byte) AIS_AID.length;
		byte[] SEL_BY_AID_HEAD = new byte[] { 0x00, // CLA
				(byte) 0xA4, // SELECT
				0x04, // P1 in binariio deve valere 0000 0100 -> Select by
						// AID
				0x0C // P2 in binario deve valere 0000 1100 -> No Data in
						// response
		};
		try {
			baos.write(SEL_BY_AID_HEAD);
			baos.write(AID_LENGTH);
			baos.write(AIS_AID);
			selByAIDcmd = new CommandAPDU(baos.toByteArray());
			baos.close();
		} catch (IOException e) {
			throw new BuildCommandException(e);
		} finally {
			ResourceHandler.safeClose(baos);
		}
		ResponseAPDU selectAISApplet = ch.transmit(selByAIDcmd);
		logResp("Root selection", selectAISApplet);
	}
	
	
	/**
	 * Select the CIE Applet from current root, current file system selection is lost
	 * @throws CardException
	 * @throws BuildCommandException
	 */
	public void selectCIEApplet() throws CardException, BuildCommandException {
		APDUFileSystemCmd.selectRoot();
		this.selectAISApplet();
		this._selectCIEApplet();
	}
	
	/**
	 * Select the CIE Applet from current File System selection
	 * @throws CardException
	 * @throws BuildCommandException
	 */
	private void _selectCIEApplet() throws CardException, BuildCommandException {
		ResponseAPDU selectCIEApplet = ch.transmit(APDUFileSystemCmd.selectApplet(CIEFileSystmeCmd.CIE_AID));
		logResp("SEL CIE APPLET", selectCIEApplet);
	}

	/**
	 * Read the ID_Servizi from current file system selection
	 * @return the String for ID_Servizi
	 * @throws CardException
	 */
	private String _readID_Servizi() throws CardException {
		ResponseAPDU readID_Servizi = ch
				.transmit(APDUFileSystemCmd.readBySFI(SFI.ID_SERVIZI, (byte) 0x0C, (byte) 0x00));
		logResp("_readID_Servizi", readID_Servizi);
		return DatatypeConverter.printHexBinary(readID_Servizi.getData());
	}

	

	/**
	 * Read the id servizi from the CIE. IAS and CIE are selected inside the
	 * method, after invocation the current file system position is lost
	 * 
	 * @return the ID_Servizi
	 * @throws BuildCommandException
	 * @throws CardException
	 */
	public String readID_Servizi() throws BuildCommandException, CardException {
		APDUFileSystemCmd.selectRoot();
		this._selectAISApplet();
		this._selectCIEApplet();
		return this._readID_Servizi();
	}
	
	
	/**
	 * Read the ATR starting from root, current file system selection is lost
	 * @return the String for ATR
	 * @throws BuildCommandException
	 * @throws CardException
	 */
	public String readATR() throws BuildCommandException, CardException{
		APDUFileSystemCmd.selectRoot();
		this._selectAISApplet();
		return this._readATR();
	}
	
	/**
	 * Read the ATR from current file system selection
	 * @return the String for ATR
	 * @throws CardException
	 */
	private String _readATR() throws CardException{
		ResponseAPDU _readATR = ch.transmit(APDUFileSystemCmd.readBySFI(SFI.ATR, (byte)0xFF, (byte)0x00));
		logResp("_readATR", _readATR);
		return DatatypeConverter.printHexBinary(_readATR.getData());
	}
	
	public String readCardSerialNumber() throws CardException, BuildCommandException{
		APDUFileSystemCmd.selectRoot();
		this._selectAISApplet();
		return this._readCardSerialNumber();
	}
	
	private String _readCardSerialNumber() throws CardException, BuildCommandException{
		ResponseAPDU selEF_SN_ICC = ch.transmit(APDUFileSystemCmd.selectByEFID(EFID.SN_ICC));
		logResp("selEF_SN_ICC", selEF_SN_ICC);
		ResponseAPDU SN_ICC = ch.transmit(APDUFileSystemCmd.readSelectedEF((byte)0x0C, (byte)0x00, (byte)0x00));
		logResp("read SN_ICC", SN_ICC);
		return DatatypeConverter.printHexBinary(SN_ICC.getData()) ;
	}
	
	// TODO read and expose in proper format all cryptographic elements
	/*
	CommandAPDU selTest = APDUFileSystemCmd.selectApplet(CIEFileSystmeCmd.AIS_AID);
		selTestHans = ch.transmit(selTest);
		System.out.println("sel cie aid " + selTestHans);

		selTestHans = ch.transmit(APDUFileSystemCmd.readBySFI(SFI.DH, (byte)0xFF, (byte)0x00));
		System.out.println("DH " + selTestHans);
		System.out.println("DH DATA " + DatatypeConverter.printHexBinary(selTestHans.getData()));
		
		
		selTestHans = ch.transmit(APDUFileSystemCmd.readBySFI(SFI.ID_SERVIZI, (byte)0x0C, (byte)0x00));
		System.out.println("ID_SERVIZI " + selTestHans);
		System.out.println("ID_SERVIZI DATA " + DatatypeConverter.printHexBinary(selTestHans.getData()));
		
		selTestHans = ch.transmit(APDUFileSystemCmd.readBySFI(SFI.INT_KPUB, (byte)0xFF, (byte)0x00));
		System.out.println("INT_KPUB " + selTestHans);
		System.out.println("INT_KPUB DATA " + DatatypeConverter.printHexBinary(selTestHans.getData()));
		
		selTestHans = ch.transmit(APDUFileSystemCmd.readBySFI(SFI.SERVIZI_INT_KPUB, (byte)0xFF, (byte)0x00));
		System.out.println("SERVIZI_INT_KPUB " + selTestHans);
		System.out.println("SERVIZI_INT_KPUB DATA " + DatatypeConverter.printHexBinary(selTestHans.getData()));

		
		
		byte[] a = DatatypeConverter.parseHexBinary("3082010A0282010100B47B1396988138974EA6F412677BEE9EDB819B483F7AFB847085E49ABC9C340A3F132D91EB2DA50AAD1A54C4FD1132E036F0253B6160FECF5A86FAB7C1CC6B117AA800E88DBFEADBDF72E2C84E9A462A93C79FC55809D7FC9264EB0A7D3E16B187C24BAB751363BDD2D0D37015BC86AB28DF0681DBB42113ED66D5FE9359D6B45A919B86758F5180C7054083B8C5DD68124675B8781F25036767804D8E7C3BAC1893B99AC195B8732EEE9FAF827D9B609AB075B85FFB7E8629EDF82FAC18D5A47DA0A61ABE69C24B341984040FB3C4BB2EE8279F618337455652A8092865");
		
		// These will throw exception in case of type mismatch
		ASN1Sequence sequence = ASN1Sequence.getInstance(a);
		ASN1Integer modulus = ASN1Integer.getInstance(sequence.getObjectAt(0));
		ASN1Integer exponent = ASN1Integer.getInstance(sequence.getObjectAt(1));
		RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus.getPositiveValue(),
		        exponent.getPositiveValue());
		KeyFactory factory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = factory.generatePublic(keySpec);

		System.out.println(publicKey.toString());
		
	 * 
	 */
	
	/**
	 * System out message and APDU response only if log is enabled
	 * @param msg
	 * @param resp
	 */
	private void logResp(String msg, ResponseAPDU resp) {
		if(this.isDebugEnabled){
			System.out.println(msg + " -> " + resp);
		}
	}
	
	
}
