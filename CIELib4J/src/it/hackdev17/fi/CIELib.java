package it.hackdev17.fi;

import java.util.Iterator;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

import it.hackdev17.fi.cie.command.filemanagement.CIEFileSystmeCmd;
import it.hackdev17.fi.cie.exception.BuildCommandException;

/**
 * Demo class for library usage
 * 
 * @author HoochDeveloper (aka Michele Salvatore Rillo) 2017
 */
public class CIELib {

	public static void main(String args[]) {
		try {
			new CIELib().readTerminal();
		} catch (CardException e) {
			e.printStackTrace();
		} catch (BuildCommandException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Infinite Loop that reads the card, ask to remove card and wait for a new
	 * read
	 * 
	 * @throws CardException
	 * @throws BuildCommandException
	 */
	private void readTerminal() throws CardException, BuildCommandException {

		TerminalFactory tf = TerminalFactory.getDefault();
		List<CardTerminal> terminals = tf.terminals().list();
		// terminals may be empty:
		// on windows: start smartcard service (from windoes services)
		// on linux: start the pcscd daemon and check the args to run java for
		// libpcsc link
		int selectedTerminal = -1;
		if (terminals.size() > 1) {
			while (selectedTerminal >= terminals.size() || selectedTerminal < 0) {
				System.out.println("Please select the terminal");
				Iterator<CardTerminal> itr = terminals.iterator();
				while (itr.hasNext()) {
					CardTerminal t = itr.next();
					System.out.println(t.getName());
				}
				String input = System.console().readLine();
				int read = Integer.parseInt(input);
				if (read >= terminals.size() || read < 0) {
					System.out.println(
							"Invalid input, please select a reader between 0 and " + terminals.size() + "- 1.");
				}
				selectedTerminal = read;
			}
		} else if (terminals.size() == 0) {
			System.out.println("No terminal");
			return;
		} else {
			selectedTerminal = 0;
		}
		CardTerminal ct = terminals.get(selectedTerminal);
		System.out.println("Reading from " + ct.getName());

		while (true) {
			System.out.println("Waiting for card");
			ct.waitForCardPresent(0);
			Card card = ct.connect("*");
			System.out.println("Connected");
			CardChannel ch = card.getBasicChannel();

			CIEFileSystmeCmd cieFS = new CIEFileSystmeCmd(ch, false);
			System.out.println("IS_Servizi " + cieFS.readID_Servizi());
			System.out.println("ATR " + cieFS.readATR());
			System.out.println("SN_ICC " + cieFS.readCardSerialNumber());

			// Disconnect the card
			card.disconnect(false);
			System.out.println("Disconnected");
			System.out.println("Please remove card");
			ct.waitForCardAbsent(0);
		}
	}

}
