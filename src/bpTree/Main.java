package bpTree;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {
	/**
	 * データを保存するファイル名
	 */
	private static final String DATA_FILENAME = "data.dat";
	/**
	 * REDOデータを保存するファイル名
	 */
	public static final String REDO_FILENAME = "redo.dat";

    public static void main(String[] args) {
    	//データを読み込む
        BPMap<Integer, Values> m = (BPMap<Integer, Values>) FileIO.read(DATA_FILENAME);

        //データがない場合はnewする
        if(m == null) {
        	m = new BPMap<>();
        }

        //REDOする処理
//        RandomAccessFile randomfile = null;
//        try {
//            //RandomAccessFileオブジェクトの生成
//            randomfile = new RandomAccessFile(REDO_FILENAME, "rw");
//
//            int pointer = 0;
//
//            while(pointer < randomfile.length()) {
//                randomfile.seek(pointer);
//                String log = randomfile.readLine();
//                if(log != null){
//                	switch (log) {
//					case "insert":
//						pointer += 12;
//						randomfile.seek(pointer);
//						int key = Integer.valueOf(randomfile.readLine());
//						pointer += 12;
//						randomfile.seek(pointer);
//						String value1 = randomfile.readLine();
//						pointer += 12;
//						randomfile.seek(pointer);
//						String value2 = randomfile.readLine();
//						m.insert(key, new Values(value1, value2));
//						break;
//					case "delete":
//						pointer += 12;
//						randomfile.seek(pointer);
//						int deleteKey = Integer.valueOf(randomfile.readLine());
//						if(m.member(deleteKey)) {
//							m.delete(deleteKey);
//						}
//						break;
//					default:
//						break;
//					}
//					pointer += 12;
//                }
//            }
//        } catch (IOException e) {
//        } finally {
//        	if(randomfile != null) {
//        		try {
//                	randomfile.close();  //RandomAccessFileストリームのクローズ
//        		} catch (IOException e) {
//				}
//        	}
//		}

        //データを保存してWALを削除
        try {
            FileIO.write(DATA_FILENAME, m);
        } catch (Exception e) {
        	return;
		}
        FileIO.fileDelete(REDO_FILENAME);

		infomation();

		//コンソールからキーボード入力を受けるオブジェクト
    	Scanner scan = new Scanner(System.in);

    	boolean flag = true;

    	Connection cn = new Connection(m);

    	//Randomクラスのインスタンス化
        Random random = new Random();

//        ArrayList<Integer> keys;
//        if(m.root == m.getNil()) {
//        	keys = new ArrayList<Integer>();
//        } else {
//        	keys = m.keys();
//        }
//        boolean hasCommand = false;
//
//        int randomNum;
//
//        int i;
//        for(i = 0; i < 400; i++) {
//        	randomNum = random.nextInt(10);
//        	if(randomNum < 4) {
////        		int key = random.nextInt(500);
//				if(cn.insert(i, randomStr(5), (random.nextInt(120) + 1900) + "")) {
//					keys.add(i);
//					hasCommand = true;
//				}
//        	} else if(randomNum < 6) {
//        		if(keys.size() != 0) {
//					if(cn.update(keys.get(random.nextInt(keys.size())), randomStr(5), (random.nextInt(120) + 1900) + "")) {
//						hasCommand = true;
//					} else {
////						break;
//					}
//				}
//        	} else if(randomNum < 9) {
//        		if(keys.size() != 0) {
//					int index = random.nextInt(keys.size());
//					if(cn.delete(keys.get(index))) {
//						keys.remove(index);
//						hasCommand = true;
//					} else {
////						break;
//					}
//				}
//        	} else {
//        		if(hasCommand) {
//					if(random.nextInt(10) < 8) {
//						cn.commit();
//					} else {
//						cn.abort();
//					}
//					hasCommand = false;
//				}
//        	}
//        }
//        if(i == 100) {
//        	System.out.println("完了");
//        } else {
//        	System.out.println(i);
//        }

//        cn.abort();

//        System.out.println(m);

//        for(i = 0; i < 100; i++) {
//        	cn.delete(i);
//        }
//        cn.commit();

        for(int i = 0; i < 15; i++) {
        	cn.insert(i, "aa", "2000");
        }
        cn.commit();

		//endされるまで処理を回し続ける
    	while(flag) {
    		try {
	    		System.out.print("コマンドを入力＞");
	    		String command = scan.next();

	    		//処理の分岐
	    		switch (command) {
				case "insert":
		        	cn.insert(scan.nextInt(), scan.next(), scan.next());
					break;
				case "update":
					cn.update(scan.nextInt(), scan.next(), scan.next());
					break;
				case "delete":
					cn.delete(scan.nextInt());
					break;
				case "commit":
					cn.commit();
					break;
				case "abort":
					cn.abort();
					break;
				case "select":
			        cn.select(scan.nextInt());
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
					int rangeNum = scan.nextInt();
					ArrayList<Values> range = m.range(rangeKey, rangeNum);
					for(Values values : range) {
						System.out.println(values.getStr() + " " + values.getNum());
					}
					break;
				case "end":
					flag = false;
					break;
				default:
					System.out.println("入力されたコマンドは存在しません");
					infomation();
					break;
				}
    		} catch (Exception e) {
    			System.out.println("エラー発生");
			}
    	}
    	try {
            FileIO.write(DATA_FILENAME, m);
    	} catch (Exception e) {
			return;
		}
        FileIO.fileDelete(REDO_FILENAME);
    }

    private static void infomation() {
		System.out.println("コマンド一覧");
		System.out.println("insert キー(Integer) 値1(String) 値2(Integer)");
		System.out.println("delete キー(Integer)");
		System.out.println("select キー(Integer)");
		System.out.println("range キー(Integer) 範囲(Integer)");
		System.out.println("tree(木構造確認)");
		System.out.println("end(終了)");
		System.out.println();
    }

    /**
     * 引数で指定された数の文字列を返す
     * @param length 作成したい文字数
     * @return 作成された文字列
     */
    private static String randomStr(int length) {
    	String result = "";
    	int num;
    	char c;

        //Randomクラスのインスタンス化
        Random random = new Random();

        for(int i = 0; i < 5; i++){
            //０～２５の乱数に６５を足して６５～９０にする
            num = 65 + random.nextInt(26);

            //charに型変換
            c = (char) num;

            //返す文字列に連結
            result += String.valueOf(c);
        }
    	return result;
    }
}
