/**
 * 
 */
 
package universe.constellation.orion.viewer.device;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author vldmr
 *
 */
public class EdgeKbdThread extends Thread {
    
    KeyEventProducer producer;
    
    public EdgeKbdThread(KeyEventProducer producer) {
        this.producer = producer;
    }
    
    public void run() {
        FileReader f;
		try {
			f = new FileReader("/dev/input/event1");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
        int sizeof_input_event = 16;
        int c;
        int input_event[] = new int[sizeof_input_event]; // struct input_event
        int n;
        while (true) {
            try {
                n = 0;
				for (int i=0; i<sizeof_input_event; i++) {
				        if (n == 0)
				            c = f.read();
				        else {
				            if (!f.ready())
				                // someone have stolen or event - just drop it
				                break;
                            c = f.read();
                        }
						input_event[i] = c;
						n++;
                }
                if (n < sizeof_input_event)
                    // event was stolen, forget it
                    continue;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
            if (input_event[8] != 0x01 /* input_event.type != EV_KEY */)
                continue;
            if (input_event[12] != 0x01 /* input_event.value != 1 -- key press */)
                continue;
            switch (input_event[10] /* input_event.code */) {
                case 109:
                    producer.nextPage();
                    break;
                case 104:
                    producer.prevPage();
                    break;
            }
        }
    }
}
