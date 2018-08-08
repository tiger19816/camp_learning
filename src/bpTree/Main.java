package bpTree;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

    	Scanner scan = new Scanner(System.in);

    	boolean flag = true;

        String basepath = System.getProperty("user.dir");
        String fileName = basepath + "data.dat";
        BPMap<Integer, Values> m = (BPMap<Integer, Values>) FileIO.read(fileName);

        if(m == null) {
        	m = new BPMap<>();
        }

    	while(flag) {

    		System.out.print("コマンドを入力：");
    		String command = scan.next();

    		switch (command) {
			case "insert":
				System.out.println("insertモード");
				while(true) {
					System.out.println("keyとvalueを入力(keyを-1でコマンド入力に戻る)");
		        	int key = scan.nextInt();
		        	if(key == -1) {
			        	break;
		        	}
		        	String value1 = scan.next();
		        	String value2 = scan.next();
			        m.insert(key, new Values(value1, value2));
				}
				break;
			case "select":
				try {
					System.out.print("値を取り出したいキーを入力：");
		        	int key = scan.nextInt();
		        	Values values = m.lookup(key);
					System.out.println("値1：" + values.getStr());
					System.out.println("値2：" + values.getNum());
				} catch(NullPointerException ex) {
					System.out.println("入力されたキーは存在しません");
				}
				break;
			case "delete":
				System.out.print("削除したいキーを入力：");
	        	int key = scan.nextInt();
				if(m.member(key)) {
					m.delete(key);
				} else {
					System.out.println("キーが存在しません");
				}
				break;
			case "tree":
				if(!m.isEmpty()) {
					System.out.println(m);
					System.out.println();
			        System.out.println("size: " + m.size());
			        System.out.println("keys: " + m.keys());
			        System.out.println("values: " + m.values());
				} else {
					System.out.println("空です");
				}
				break;
			case "range":
				int rangeKey = scan.nextInt();
				ArrayList<Values> range = m.range(rangeKey, 3);
				for(Values values : range) {
					System.out.println(values.getStr() + " " + values.getNum());
				}
				break;
			case "end":
				flag = false;
				break;
			default:
				System.out.println("入力されたコマンドは存在しません");
				System.out.println("insert");
				System.out.println("select");
				System.out.println("delete");
				System.out.println("tree");
				System.out.println("end");
				break;
			}
    	}

        FileIO.write(fileName, m);
    }
}
