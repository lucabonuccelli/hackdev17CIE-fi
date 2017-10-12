C++ = g++
BINDIR = ./bin/
FLAGS = -std=c++11 -g -I/usr/include/PCSC/ -lpcsclite
BINNAME = test-verify

all: objs $(BINNAME)
objs: example-verify.o requests.o
	
$(BINNAME):
	mkdir -p $(BINDIR)
	$(C++) $(FLAGS) -o $(BINDIR)$(BINNAME) example-verify.o requests.o
	rm *.o

example-verify.o:
	$(C++) $(FLAGS) -c example-verify.cpp

requests.o:
	$(C++) $(FLAGS) -c requests.cpp

clean:
	rm -f $(BINDIR) *.o
