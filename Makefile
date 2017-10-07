C++ = g++
FLAGS = -I /usr/include/PCSC -lpcsclite

all: objs test
objs: example.o requests.o
	
test:
	$(C++) $(FLAGS) -o test example.o requests.o
	rm *.o

example.o:
	$(C++) $(FLAGS) -c example.cpp

requests.o:
	$(C++) $(FLAGS) -c requests.cpp

clean:
	rm test
