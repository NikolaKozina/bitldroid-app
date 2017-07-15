android-bitlbee: *
	ant debug
install:
	ant installd
clean:
	rm -r gen/
	rm -r bin/
	rm -r libs/
tags: 
	ctags *
.DUMMY: tags clean install
