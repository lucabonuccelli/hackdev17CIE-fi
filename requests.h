#ifndef REQUESTS_GUARD
#define REQUESTS_GUARD

namespace Requests
{
const std::size_t RESPONSE_SIZE {300};

/*
 * Sends the specified apdu, which must be encapsulated in a BYTE vector,
 * to the specified card handle.
 * The response is stored in the response array.
 * On success, 1 is returned, otherwise 0 is returned.
 */
int send_apdu(const SCARDHANDLE &card, const std::vector<BYTE> &apdu,
		std::vector<BYTE> &response);

}
#endif
