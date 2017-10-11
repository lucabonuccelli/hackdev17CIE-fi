package it.hackdev17.fi.cie.command.filemanagement;

public class EFID {

	public static final byte[] DH = {(byte)0xD0, 0x04};
	public static final byte[] ATR = {(byte)0x2F, 0x01};
	public static final byte[] SN_ICC = {(byte)0xD0, 0x03};	
	
	public static final byte[] ID_SERVIZI = {0x10,0x01};
	public static final byte[] SERIALE = {0x10,0x02};
	public static final byte[] CERT_CIE = {0x10,0x03};
	public static final byte[] INT_KPUB = {0x10,0x04};
	public static final byte[] SERVIZI_INT_KPUB = {0x10,0x05};
	public static final byte[] SOD = {0x10,0x06};
	public static final byte[] CIE_KPUB = {0x10,0x07};
	
	
	private EFID() {
	}

}
