package it.hackdev17.fi.cie.command.filemanagement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.smartcardio.CommandAPDU;

import it.hackdev17.fi.cie.exception.BuildCommandException;
import it.hackdev17.fi.utils.ResourceHandler;


/**
 * Helper class for sending generic APDU message.
 * APDU command are compliant with the CIE specification.
 * @author HoochDeveloper (aka Michele Salvatore Rillo)
 * 2017
 */
public class APDUFileSystemCmd {

	/**
	 * 1110 0000
	 */
	private static final byte MASKF3 = (byte) 0xE0;
	/**
	 * 0001 1111
	 */
	private static final byte MASKL5 = (byte) 0x1F;	

	/**
	 * @return APDU command for select file system root
	 */
	public static CommandAPDU selectRoot() {
		byte[] SEL_ROOT = new byte[] { 0x00, // CLA -> ISO COMMAND
				(byte) 0xA4, // INS SELECT
				0x00, // P1 SELECT ROOT 0000 0000
				0x0C, // P2 No response
				0x02, // LC EFID LENGTH 2 bytes
				// data DFID ROOT = 3F00
				(byte) 0x3F, // first part
				(byte) 0x00, // second part
				// data end
				0x00 // 0 response length

		};
		return new CommandAPDU(SEL_ROOT);
	}

	/**
	 * @param AID Application Identifier for the applet
	 * @return APDU command for selecting the applet corresponding to the input AID
	 * @throws BuildCommandException
	 */
	public static CommandAPDU selectApplet(byte[] AID) throws BuildCommandException {
		CommandAPDU selByAIDcmd = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte AID_LENGTH = (byte) AID.length;
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
			baos.write(AID);
			selByAIDcmd = new CommandAPDU(baos.toByteArray());
			baos.close();
		} catch (IOException e) {
			throw new BuildCommandException(e);
		} finally {
			ResourceHandler.safeClose(baos);
		}
		return selByAIDcmd;
	}

	

	/**
	 * @param EIFD identifier for elementary file to select
	 * @return APDU command for selecting the EF corresponding to the input EFID
	 * @throws BuildCommandException
	 */
	public static CommandAPDU selectByEFID(byte[] EIFD) throws BuildCommandException {
		CommandAPDU selectByEFID;
		byte[] SEL_BY_EFID = new byte[] { 0x00, // CLA -> ISO
				(byte) 0xA4, // INS READ
				(byte) 0x02, // P1 SEECT BY EFID
				(byte) 0x0C, // P2 in binario deve valere 0000 1100 -> No Data
								// in response
				(byte) EIFD.length // LC LENGHT OF DATA FIELD
		};
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(SEL_BY_EFID);
			baos.write(EIFD); // DATA FIELD
			baos.write(0x00); // LE LENGTH OF DATA IN RESPONSE
			selectByEFID = new CommandAPDU(baos.toByteArray());
			baos.close();
		} catch (IOException e) {
			throw new BuildCommandException(e);
		} finally {
			ResourceHandler.safeClose(baos);
		}
		return selectByEFID;

	}

	/**
	 * @param DFID
	 * @return APDU commnad for selecting child of input DFID
	 * @throws BuildCommandException
	 */
	public static CommandAPDU selectChildDF(byte[] DFID) throws BuildCommandException {
		CommandAPDU selChildDf;
		byte[] SEL_CHILD_DF_HEAD = new byte[] { 0x00, // CLA -> ISO
				(byte) 0xA4, // INS READ
				(byte) 0x01, // P1 SEECT CHILD DF
				(byte) 0x0C, // P2 in binario deve valere 0000 1100 -> No Data
								// in response
				(byte) DFID.length, // LC LENGHT OF DATA FIELD
		};
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(SEL_CHILD_DF_HEAD);
			baos.write(DFID); // DATA FIELD
			baos.write(0x00); // LE LENGTH OF DATA IN RESPONSE
			selChildDf = new CommandAPDU(baos.toByteArray());
			baos.close();
		} catch (IOException e) {
			throw new BuildCommandException(e);
		} finally {
			ResourceHandler.safeClose(baos);
		}
		return selChildDf;
	}

	
	/**
	 * @param lengthToRead byte to read
	 * @param MSOffset most significant bit for offset
	 * @param LSOffset less significant bit for offset
	 * @return APDU read binary command for the current selected EF
	 */
	public static CommandAPDU readSelectedEF(byte lengthToRead, byte MSOffset, byte LSOffset) {
		
		byte[] READ_CURR_EF = new byte[] { 0x00, // CLA -> ISO
				(byte) 0xB0, // INS READ
				MSOffset, // P1 offset most significant bit
				LSOffset, // P2 Offset part 2
				lengthToRead // LE bytes to read
		};
		return new CommandAPDU(READ_CURR_EF);

	}
	
	
	/**
	 * @param SFI short file identifier to read
	 * @param lengthToRead byte to read
	 * @param offset start read from byte
	 * @return APDU read binary command for the input Short File Identifier
	 */
	public static CommandAPDU readBySFI(byte SFI, byte lengthToRead, byte offset) {
		byte P1 = (byte) ((0x80 & MASKF3) | (SFI & MASKL5)); // 0x80 is 1000
																// masked by
																// 0xE0 become
																// 100 -----
		byte[] READ_BY_SFI = new byte[] { 0x00, // CLA -> ISO
				(byte) 0xB0, // INS READ
				(byte) P1, // P1 SELECT BY SFI 100 + xxxxx since SFI is 1B
							// 0001 1011 then become 1001 1011 - > 9B
							// hex
				offset, // P2 Offset in file
				lengthToRead // LE bytes to read
		};
		return new CommandAPDU(READ_BY_SFI);

	}
	
	private APDUFileSystemCmd() {
	};

}
