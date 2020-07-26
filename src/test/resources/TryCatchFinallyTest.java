import java.io.IOException;
import java.util.Random;

class TryCatchFinallyTest {

    void test1() {
        try {
            throw new IOException();
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }

    void test2() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    void test3() {
        IllegalAccessError e = new IllegalAccessError();
        try {
            throw e;
        } catch (NullPointerException p) {
            throw new IndexOutOfBoundsException();
        } finally {

        }
    }

    void test4() throws IOException {
        IllegalAccessError e = new IllegalAccessError();
        try {
            System.out.println("hi");
            throw e;
        } catch (Exception w) {
            throw new IndexOutOfBoundsException();
        } finally {
            throw new IOException();
        }
    }

    void test5() {
        IOException f = new IOException();
        IndexOutOfBoundsException g = new IndexOutOfBoundsException();
        try {
            try {
                try {
                    throw f;
                } catch (IOException h) {

                } finally {
                    throw g;
                }
            } catch (IndexOutOfBoundsException t) {
                try {
                    throw new Exception();
                } catch (Exception y) {
                    if (new Random().nextBoolean()) {
                        throw new InterruptedException();
                    } else {
                        throw new RuntimeException();
                    }
                }
            }
        } catch (InterruptedException s) {
            throw new StackOverflowError();
        } catch (RuntimeException a) {

        } finally {

        }
    }

    void test6() throws IOException, IllegalAccessException {
        if (new Random().nextBoolean()) {
            throw new IllegalAccessException();
        } else {
            throw new IOException();
        }
    }

    void test7() throws IllegalAccessException {
        try {
            if (new Random().nextBoolean()) {
                throw new IllegalAccessException();
            } else {
                throw new IOException();
            }
        } catch (IOException e) {

        } finally {

        }
    }

    void test8() {
        try {
            System.out.println("hi");
            throw new MyException();
        } catch (Error p) {
            throw new IndexOutOfBoundsException();
        } finally {

        }
    }
}