#include <iostream>
#include <sstream>
#include <vector>
#include <winscard.h>
#include <ios>
#include "requests.h"

bool Requests::send_apdu(const SCARDHANDLE &card,
		const std::vector<BYTE> &apdu, std::vector<BYTE> &response)
{
	DWORD resp_len {RESPONSE_SIZE};
	SCardTransmit(card, SCARD_PCI_T1, apdu.data(), apdu.size(),
			NULL, response.data(), &resp_len);
	// verifica che la Status Word sia 9000 (OK)
	for (auto &x : response)
		std::cout << std::hex << x << ' ';
	std::cout << std::hex << response[resp_len -2];
	if (response[resp_len - 2] != 0x90 || response[resp_len - 1] != 0x00) {
		std::cerr << "Errore nella lettura della risposta\n";
		return false;
	}
	std::cout << std::endl;
	return true;
}

bool Requests::read_nis(const SCARDHANDLE &card, std::vector<BYTE> &response)
{
	std::vector<BYTE> readNIS = {0x00, // CLA
		0xb0, // INS = READ BINARY
		0x81, // P1 = Read by SFI & SFI = 1 //to read public key
		0x00, // P2 = Offset = 0
		0x0c  // LE = length of NIS
	};
	// invia l'APDU
	if (!Requests::send_apdu(card, readNIS, response)) {
		std::cerr << "Errore nella lettura dell'Id_Servizi\n";
		return false;
	} 
	return true;
}

int Requests::create_apdu(std::vector<BYTE> &apdu)
{
	std::string apdu_string {};
	std::cout << "Inserisci i valori dell'APDU: ";
	std::cout.flush();
	std::cin >> apdu_string;
	//std::getline(std::cin, apdu_string);
	std::istringstream sstream {apdu_string};

	for (std::string tmp {}; std::getline(sstream, tmp, '-' );)
		apdu.push_back((BYTE) std::stoi(tmp));
	std::cout << apdu_string << '\n';
	std::cout << "Invio dell'APDU personalizzata..." << '\n';
	return 1;
}
