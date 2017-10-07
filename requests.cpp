#include <iostream>
#include <vector>
#include <winscard.h>
#include "requests.h"

int Requests::send_apdu(const SCARDHANDLE &card,
		const std::vector<BYTE> &apdu, std::vector<BYTE> &response)
{
	DWORD resp_len {RESPONSE_SIZE};
	SCardTransmit(card, SCARD_PCI_T1, apdu.data(), apdu.size(),
			NULL, response.data(), &resp_len);
	// verifica che la Status Word sia 9000 (OK)
	if (response[resp_len - 2] != 0x90 || response[resp_len - 1] != 0x00) {
		std::cerr << "Errore nella lettura della risposta\n";
		return 0;
	}
	return 1;
}
