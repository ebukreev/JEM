import java.io.IOException;
import java.util.Random;

class TryCatchFinallyTest {



    void bigTest() {
        IOException f = new IOException();
        IndexOutOfBoundsException g = new IndexOutOfBoundsException();
        System.out.println("hi");
        try {
            try {
                try {
                    throw f;
                } catch (IOException e) {

                } finally {
                    throw g;
                }
            } catch (IndexOutOfBoundsException t) {
                try {
                    throw new Exception();
                } catch (Exception y) {
                    if (new Random().nextBoolean()) {
                        throw new MyException();
                    } else {
                        throw new RuntimeException();
                    }
                }
            }
        } catch (MyException s) {
            throw new StackOverflowError();
        } catch (RuntimeException a) {

        } finally {

        }
    }
}