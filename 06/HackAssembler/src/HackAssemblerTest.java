import java.io.IOException;

import org.junit.Test;


public class HackAssemblerTest {

	@Test
	public void test() throws IOException {
		final String filename = "res/pong/Pong.asm"; 
		
		HackAssembler hack = new HackAssembler();
		hack.open(filename);
		hack.assemble();
	}

}
