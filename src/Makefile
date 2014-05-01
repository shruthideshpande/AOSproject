.SUFFIXES: .java .class
.java.class:
	javac aosproject.java

CLASSES = aosproject.java

default: classes

classes: $(CLASSES:.java=.class)
	echo "Done Compiling!"

clean:
	$(RM) *.class
