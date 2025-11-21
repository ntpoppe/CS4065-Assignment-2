JC = javac
JR = java
OUT_DIR = out/production/server

SOURCES = $(wildcard src/server/*.java)

all: classes

classes:
	$(JC) -d $(OUT_DIR) $(SOURCES)

run: all
	$(JR) -cp $(OUT_DIR) server.Main

clean:
	rm -rf $(OUT_DIR)
