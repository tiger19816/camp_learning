package bpTree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.zip.CRC32;

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

        //DBと接続するConnectionクラス
    	Connection cn = new Connection(m);

    	if(FileIO.fileExists(REDO_FILENAME)) {

	        //REDOする処理
	        RandomAccessFile randomfile = null;
	        try {
	            //RandomAccessFileオブジェクトの生成
	            randomfile = new RandomAccessFile(REDO_FILENAME, "r");

	            if(randomfile.length() != 0) {
		            CRC32 crc = new CRC32();

		            int pointer = 0;
		            while(pointer < randomfile.length()) {
		                randomfile.seek(pointer);
		                long checksum = randomfile.readLong();

		                pointer += 8;
		                randomfile.seek(pointer);
		                String log = randomfile.readLine();

		                byte[] buffer = log.getBytes();
		                crc.reset();
		                crc.update(buffer);
		                long logChecksum = crc.getValue();

		                if(checksum == logChecksum) {
		                	int num = 0;
		                	int next = log.indexOf(" ");
		                	String command = log.substring(num, next);
		                	num = next + 1;
		                	switch (command) {
							case "insert":
								next = log.indexOf(" ", num);
								int key = Integer.valueOf(log.substring(num, next));

								num = next + 1;
								next = log.indexOf(" ", num);
								String value1 = log.substring(num, next);

								num = next + 1;
								next = log.indexOf(" ", num);
								String value2 = log.substring(num, next);

								cn.insert(key, value1, value2);
								break;
							case "update":
								next = log.indexOf(" ", num);
								key = Integer.valueOf(log.substring(num, next));

								num = next + 1;
								next = log.indexOf(" ", num);
								value1 = log.substring(num, next);

								num = next + 1;
								next = log.indexOf(" ", num);
								value2 = log.substring(num, next);

								cn.update(key, value1, value2);
								break;
							case "delete":
								next = log.indexOf(" ", num);
								key = Integer.valueOf(log.substring(num, next));

								cn.delete(key);
								break;
							case "commit":
								cn.redoCommit();
								break;
							default:
								break;
							}
		    				pointer += 40;
		                } else {
		                	break;
		                }
		            }
	            }
	        } catch (IOException e) {
	        } finally {
	        	if(randomfile != null) {
	        		try {
	                	randomfile.close();  //RandomAccessFileストリームのクローズ
	        		} catch (IOException e) {
					}
	        	}
			}
    	}

        //データを保存してWALを削除
        try {
            FileIO.write(DATA_FILENAME, m);
        } catch(Exception e) {
        	return;
		}
        try {
        	FileIO.clearFile(REDO_FILENAME);
        } catch(Exception e) {
        }

        cn.abort();

    	//Randomクラスのインスタンス化
        Random random = new Random();

        ArrayList<Integer> keys;
        if(m.root == m.getNil()) {
        	keys = new ArrayList<Integer>();
        } else {
        	keys = m.keys();
        }
        boolean hasCommand = false;

        int randomNum;

        int i;
        for(i = 0; i < 1; i++) {
        	randomNum = random.nextInt(10);
        	if(randomNum < 5) {
//        		int key = random.nextInt(500);
				if(cn.insert(i, randomStr(5), (random.nextInt(120) + 1900) + "")) {
					keys.add(i);
					hasCommand = true;
				}
        	} else if(randomNum < 7) {
        		if(keys.size() != 0) {
					if(cn.update(keys.get(random.nextInt(keys.size())), randomStr(5), (random.nextInt(120) + 1900) + "")) {
						hasCommand = true;
					}
				}
        	} else if(randomNum < 9) {
        		if(keys.size() != 0) {
					int index = random.nextInt(keys.size());
					if(cn.delete(keys.get(index))) {
						keys.remove(index);
						hasCommand = true;
					}
				}
        	} else {
        		if(hasCommand) {
					if(random.nextInt(10) < 8) {
						try {
							cn.commit();
						} catch (Exception e) {
						}
					} else {
						cn.abort();
					}
					hasCommand = false;
				}
        	}
        }

    	//fffff
        if(hasCommand) {
        	try {
        		cn.commit();
        	} catch (Exception e) {
			}
        	hasCommand = false;
        }
        cn.abort();

        System.out.println(m);

		//コンソールからキーボード入力を受けるオブジェクト
    	Scanner scan = new Scanner(System.in);

    	boolean flag = true;

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
        try {
        	FileIO.clearFile(REDO_FILENAME);
        } catch(Exception e) {
        }
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


//	public static void main(String[] args) {
//		Random random = new Random();
//
//		BPMap<Integer, String> m = new BPMap<>();
//
//		//挿入する最大値
//		int num = 5;
//
//		//乱数の重複を確認
//		boolean[] flag = new boolean[num];
//
//		int key = 0;
//
//		System.out.println("*insert*******************");
//		for(int i = 0; i < num; i++) {
//			do {
//				key = random.nextInt(num);
//			} while(flag[key]);
//			flag[key] = true;
//			m.insert(key + 1, "value" + key);
//			System.out.println((i + 1) + "回目");
//			System.out.println(m);
//			System.out.println();
//		}
//
//		System.out.println("*select*******************");
//		for(int i = 0; i < num; i++) {
//			System.out.println(m.lookup(i + 1));
//		}
//		System.out.println();
//
//		flag = new boolean[num];
//
//		System.out.println("*delete*******************");
//		for(int i = 0; i < num; i++) {
//			do {
//				key = random.nextInt(num);
//			} while(flag[key]);
//			flag[key] = true;
//			System.out.println((key + 1) + "を削除");
//			m.delete(key + 1);
////			System.out.println((i + 1) + "を削除");
////			m.delete(i + 1);
//			System.out.println(m);
//			System.out.println();
//		}
//	}
}
