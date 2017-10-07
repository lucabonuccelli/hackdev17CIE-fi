#include <winscard.h>
#include <vector>
#include <fstream>
#include <iostream>
#include <array>
#include <string>
#include <cstring>

#define _TCHAR char

const std::size_t RESPONSE_SIZE {300};

/*
 * Sends the specified apdu, which must be encapsulated in a BYTE vector,
 * to the specified card handle.
 * The response is stored in the response array.
 * On success, 1 is returned, otherwise 0 is returned.
 */
int send_to_card(const SCARDHANDLE &card, const std::vector<BYTE> &apdu,
		std::vector<BYTE> &response);

int read_binary(const SCARDHANDLE &card, BYTE sfi, BYTE length);

int main(int argc, _TCHAR* argv[])
{
	// stibilisco la connessione al sottosistema di gestione delle smart card
	SCARDCONTEXT Context;
	SCardEstablishContext(SCARD_SCOPE_SYSTEM, NULL, NULL, &Context);

	// ottiengo la lista dei lettori installati
	char *ReaderList;
	DWORD ReaderListLen = SCARD_AUTOALLOCATE;
	SCardListReaders(Context, NULL, (char*)&ReaderList, &ReaderListLen);
	
	// inserisco i lettori in un vettore
	char* Reader = ReaderList;
	std::vector<char*> Readers;
	while (Reader[0]) {
		Readers.push_back(Reader);
		Reader += strlen(Reader) + 1;
	}

	// richiedo all'utente quale lettore utilizzare
	for (int i = 0; i < Readers.size(); i++) {
		std::cout << (i + 1) << ") " << Readers[i] << "\n";
	}
	std::cout << "Selezionare il lettore su cui è appoggiata la CIE\n";

	int ReaderNum = -1;
	std::cin >> ReaderNum;
	if (ReaderNum < 1 || ReaderNum>Readers.size()) {
		std::cout << "Lettore inesistente\n";
		return 0;
	}

	// apre la connessione al lettore selezionato, specificando l'accesso esclusivo e il protocollo T=1
	SCARDHANDLE Card;
	DWORD Protocol;
	LONG result = SCardConnect(Context, Readers[ReaderNum - 1], SCARD_SHARE_EXCLUSIVE, SCARD_PROTOCOL_T1, &Card, &Protocol);

	if (result != SCARD_S_SUCCESS) {
		std::cout << "Connessione al lettore fallita\n";
		return 0;
	}

	std::vector<BYTE> response(RESPONSE_SIZE);
	DWORD response_len {RESPONSE_SIZE};
	// prepara la prima APDU: Seleziona il DF dell'applicazione IAS
	std::vector<BYTE> selectIAS {0x00, // CLA
		0xa4, // INS = SELECT FILE
		0x04, // P1 = Select By AID
		0x0c, // P2 = Return No Data
		0x0d, // LC = length of AID
		0xA0, 0x00, 0x00, 0x00, 0x30, 0x80, 0x00, 0x00,
		0x00, 0x09, 0x81, 0x60, 0x01 // AID
	};

	// invia la prima APDU
	if (!send_to_card(Card, selectIAS, response)) {
		std::cerr << "Errore nella selezione del DF_IAS\n";
		return EXIT_FAILURE;
	} 
	// prepara la seconda APDU: Seleziona il DF degli oggetti CIE
	std::vector<BYTE> selectCIE {0x00, // CLA
		0xa4, // INS = SELECT FILE
		0x04, // P1 = Select By AID
		0x0c, // P2 = Return No Data
		0x06, // LC = length of AID
		0xA0, 0x00, 0x00, 0x00, 0x00, 0x39 // AID
	};
	// invia la seconda APDU
	if (!send_to_card(Card, selectCIE, response)) {
		std::cerr << "Errore nella selezione del DF_CIE\n";
		return EXIT_FAILURE;
	} 
	// prepara la terza APDU: Lettura del file dell'ID_Servizi selezionato contestualmente tramite Short Identifier (SFI = 1)
	std::vector<BYTE> readNIS = {0x00, // CLA
		0xb0, // INS = READ BINARY
		0x81, // P1 = Read by SFI & SFI = 1
		0x00, // P2 = Offset = 0
		0x00 // LE = length of NIS
	};
	// invia la terza APDU
	if (!send_to_card(Card, readNIS, response)) {
		std::cerr << "Errore nella lettura dell'Id_Servizi\n";
	} 

	std::ofstream out_file {"certificate.txt"};
	out_file << response.data() << "\n";
	std::cout << "Certificate written to file" << '\n';
	std::cout << "NIS: " << std::string {(char *)response.data()} << std::endl;
	SCardFreeMemory(Context, ReaderList);
	SCardDisconnect(Card, SCARD_RESET_CARD);
	return 0;
}

/*
int read_binary(const SCARDHANDLE &card, const DWORD length,
		BYTE sfi, DWORD &response_len)
{
	std::vector<BYTE> response(RESPONSE_SIZE);
	std::vector<BYTE> tmp_response {};
	BYTE tmp_len {length > 0xff ? 0xff : length};
	std::vector<BYTE> apdu {0x00, 0xb0, sfi, 0x00, tmp_len};


	while (apdu[4] > 0) {
		if (!send_to_card(card, apdu, tmp_response, response_len)) {
			std::cerr << "Errore nella lettura della risposta\n";
			return 0;
		}
		response.insert(response.end(), tmp_response.begin(),
				tmp_response.end());
		apdu[2] += response_len;
		apdu[4] -= response_len;
	}
	return 0;
}
*/

int send_to_card(const SCARDHANDLE &card, const std::vector<BYTE> &apdu,
		std::vector<BYTE> &response)
{
	const BYTE *apdu_rawdata {apdu.data()};
	const size_t apdu_size {apdu.capacity()};

	DWORD resp_len {RESPONSE_SIZE};
	SCardTransmit(card, SCARD_PCI_T1, apdu_rawdata, apdu_size,
			NULL, response.data(), &resp_len);
	// verifica che la Status Word sia 9000 (OK)
	if (response[resp_len - 2] != 0x90 || response[resp_len - 1] != 0x00) {
		std::cerr << "Errore nella lettura della risposta\n";
		return 0;
	}
	return 1;
}
