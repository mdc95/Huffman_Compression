import java.util.Arrays;
import java.util.PriorityQueue;



public class HuffProcessor implements Processor {
	String[] codes = new String[ALPH_SIZE+1];
	public void compress(BitInputStream in, BitOutputStream out) {
		int[] array = new int[ALPH_SIZE];
		int bit = in.readBits(BITS_PER_WORD);
		while (bit != -1){
			array[bit]++;
			bit = in.readBits(BITS_PER_WORD);}
		in.reset();
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for (int i = 0; i<ALPH_SIZE; i++){
			if (array[i] != 0){
				HuffNode node = new HuffNode(i, array[i]);
				pq.add(node);}}
		HuffNode fin = new HuffNode(PSEUDO_EOF, 0);
		pq.add(fin);
		int counter = 0;
		while (pq.size() > 1){
			//			poll two smallest nodes
			HuffNode first = pq.poll();
			HuffNode second = pq.poll();
			//			combine them into a new HuffNode
			HuffNode parent = new HuffNode(0, first.weight()+second.weight(), first, second);
			//add the new HuffNode into priority queue
			pq.add(parent);
			counter++;
		}
		System.out.print(counter);
		HuffNode root = pq.poll();
		extractCodes(root, "");

		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeHeader(root, out);
		//compress/write the body
		int bit2 = in.readBits(BITS_PER_WORD);
		while (bit2 != -1){
				String code = codes[bit2];
				out.writeBits(code.length(), Integer.parseInt(code, 2));
				bit2 = in.readBits(BITS_PER_WORD);
		}
		//write pseudo-eof
		String eof = codes[PSEUDO_EOF];
		out.writeBits(eof.length(), Integer.parseInt(eof, 2));
		
		
		
	}

	private void extractCodes(HuffNode current, String path){
		if (current.right() == null && current.left() == null){
			codes[current.value()] = path;
			return;}

		extractCodes(current.left(), path + 0);
		extractCodes(current.right(), path + 1);
	}

	private void writeHeader(HuffNode current, BitOutputStream out){
		if (current.right() == null && current.left() == null){
			out.writeBits(1, 1);
			out.writeBits(9, current.value());
			return;
			}
		out.writeBits(1, 0);
		writeHeader(current.left(), out);
		writeHeader(current.right(), out);
	}
	
	private HuffNode readHeader(BitInputStream in){
		if (in.readBits(1) == 0){
			 HuffNode left = readHeader(in);
			 HuffNode right = readHeader(in);
			//return combine new combined HuffNode
			 HuffNode merged = new HuffNode(0, 0, left, right);
			 return merged;}
		else{
			HuffNode oth = new HuffNode(in.readBits(9), 0);
			return oth;}
	}

	@Override
	public void decompress(BitInputStream in, BitOutputStream out) {
		//check for HUFF_NUMBER
		if (in.readBits(BITS_PER_INT) != HUFF_NUMBER){
			throw new HuffException("Error file cannot decompress");
		}//recreate tree from header
		//parse body of compressed file
		HuffNode root = readHeader(in);
		HuffNode current = root;
		int bit = in.readBits(1);
		while (bit != -1){
			if (bit == 1){
				current = current.right();}
			else{ 
				current = current.left();}
			if (current.right() == null && current.left() == null){
				if (current.value() == PSEUDO_EOF){
					return;}
				else{
					out.writeBits(8, current.value());
					current = root;}}
			bit = in.readBits(1);					}
		throw new HuffException("Error file cannot decompress");

	}

}
