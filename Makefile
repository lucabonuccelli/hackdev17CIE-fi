C++ = g++
FLAGS = -I /usr/include/PCSC -lpcsclite

all: example.o requests.o test
	
test:
	$(C++) $(FLAGS) -o test example.o requests.o

example.o:
	$(C++) $(FLAGS) -c example.cpp

requests.o:
	$(C++) $(FLAGS) -c requests.cpp

clean:
	rm test *.o
