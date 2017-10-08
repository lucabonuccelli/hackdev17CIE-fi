C++ = g++
BINDIR = ./bin/
FLAGS = -std=c++11 -g -I /usr/include/PCSC -lpcsclite

all: objs test-nis
objs: example.o requests.o
	
test-nis:
	$(C++) $(FLAGS) -o $(BINDIR)test-nis example.o requests.o
	rm *.o

example.o:
	$(C++) $(FLAGS) -c example.cpp

requests.o:
	$(C++) $(FLAGS) -c requests.cpp

clean:
	rm -f test *.o
