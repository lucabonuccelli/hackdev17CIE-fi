#ifndef REQUESTS_GUARD
#define REQUESTS_GUARD

namespace Requests
{
const std::size_t RESPONSE_SIZE {300};

/*
 * Sends the specified apdu, which must be encapsulated in a BYTE vector,
 * to the specified card handle.
 * The response is stored in the response vector.
 * In case of a successful reply, true is returned, false otherwise.
 */
bool send_apdu(const SCARDHANDLE &card, const std::vector<BYTE> &apdu,
		std::vector<BYTE> &response);

/*
 * Sends a request to read NIS from the specified SCARDHANDLE.
 * Response is stored in the response vector.
 *
 * In case of a successful reply, true is returned, false otherwise.
 */
bool read_nis(const SCARDHANDLE &card, std::vector<BYTE> &response);

/*
 * Propts the user for single APDU values and stores the newly
 * created APDU in the apdu vector. Fields must be separated
 * by a '-'.
 * On success, 1 is returned.
 */
int create_apdu(std::vector<BYTE> &apdu);
}
#endif
