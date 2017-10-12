#include <vector>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <cstring>
#include <thread>
#include <ios>
#include <cassert>
#include <PCSC/winscard.h>

#include "requests.h"

#define _TCHAR char


int main(int argc, _TCHAR* argv[])
{
	// stibilisco la connessione al sottosistema di gestione delle smart card
	SCARDCONTEXT Context;
	SCardEstablishContext(SCARD_SCOPE_SYSTEM, NULL, NULL, &Context);

	// ottiengo la lista dei lettori installati
	char *ReaderList;
	DWORD ReaderListLen = SCARD_AUTOALLOCATE;
	SCardListReaders(Context, NULL, (char *) &ReaderList, &ReaderListLen);
	
	// inserisco i lettori in un vettore
	char* Reader{ReaderList};
	std::vector<char*> Readers;
	while (Reader[0]) {
		Readers.push_back(Reader);
		Reader += strlen(Reader) + 1;
	}
	// richiedo all'utente quale lettore utilizzare
	for (int i = 0; i < Readers.size(); ++i) {
		std::cout << (i + 1) << ") " << Readers[i] << '\n';
	}
	std::cout << "Selezionare il lettore su cui è appoggiata la CIE" << '\n';

	int ReaderNum{-1};
	std::cin >> ReaderNum;
	if (ReaderNum < 1 || ReaderNum>Readers.size()) {
		std::cout << "Lettore inesistente\n";
		return -1;
	}
	// apre la connessione al lettore selezionato, specificando l'accesso esclusivo e il protocollo T=1
	SCARDHANDLE card;
	DWORD protocol;
	LONG result = SCardConnect(Context, Readers[ReaderNum - 1],
			SCARD_SHARE_EXCLUSIVE, SCARD_PROTOCOL_T1, &card,
			&protocol);

	if (result != SCARD_S_SUCCESS) {
		std::cout << "Connessione al lettore fallita\n";
		return -1;
	}
	std::vector<BYTE> response(Requests::RESPONSE_SIZE);
	// prepara la prima APDU: Seleziona il DF dell'applicazione IAS
	assert(Requests::select_df_ias(card, response));
	// prepara la seconda APDU: Seleziona il DF degli oggetti CIE
	assert(Requests::select_cie_df(card, response));
	// prepara la terza APDU: Lettura del file dell'ID_Servizi selezionato contestualmente tramite Short Identifier (SFI = 1)
	assert(Requests::read_nis(card, response));
	std::ofstream out_file{"certificate.txt"};
	out_file << response.data() << "\n";
	std::cout << "Certificato scritto su file" << '\n';
	std::cout << "NIS: " << std::string {(char *)response.data()} << std::endl;

	std::vector<BYTE> apdu{};
	bool is_good_response{true};
	while (is_good_response) {
		Requests::create_apdu(apdu);
		is_good_response = Requests::send_apdu(card, apdu, response);
		std::cout << std::endl;
		if (!is_good_response)
			std::cerr << "Errore nella lettura dell'APDU personalizzata\n";
		std::cout << "output message:" << std::string {(char *)response.data()} << std::endl;
	}
	SCardFreeMemory(Context, ReaderList);
	SCardDisconnect(card, SCARD_RESET_CARD);
	free(ReaderList);
	return 0;
}
