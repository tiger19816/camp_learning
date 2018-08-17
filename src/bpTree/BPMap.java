package bpTree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BPMap<Key extends Comparable<? super Key>, Value> implements Serializable {

	/**
	 * B+木のオーダー
	 */
	private static final int m = 5;
	/**
	 * B+木の最小分岐数[m/2]
	 */
	private static final int hm = (m + 1) / 2;

	/**
	 * ノードの型（抽象型）
	 */
	private abstract class Node implements Serializable {
		/**
		 * キーのリスト
		 */
		public List<Key> ks() {
			return null;
		}

		/**
		 * 枝のリスト
		 */
		public List<Node> ns() {
			return null;
		}

		public Node deactivate() {
			return this;
		}

		public Node trim() {
			return this;
		}

		public Node insert(Key key, Value value) {
			return this;
		}

		public void delete(Key key) {}

		public Key deleteMin() {
			return null;
		}

		public void balanceLeft(NodeInterior t, int i) {}

		public void balanceRight(NodeInterior t, int j) {}
	}

	/**
	 * nullの代わり。
	 */
	private final NodeBottom nil = new NodeBottom();

	/**
	 * B+木の根(nilは空の木)
	 */
	public Node root = nil;

	/**
	 * 挿入時のアクティブなノードか判定する
	 * @param t Node
	 * @return 挿入時のアクティブなノードならtrue
	 */
	private boolean active(Node t) {
		return t instanceof BPMap<?, ?>.NodeActive;
	}

	/**
	 * 最下層のノードか判定する
	 * @param t Node
	 * @return 最下層のノードならtrue
	 */
	private boolean bottom(Node t) {
		return t instanceof BPMap<?, ?>.NodeBottom;
	}

	/**
	 * 挿入時のアクティブなノードの型
	 */
	private class NodeActive extends Node {
		/**
		 * キーのリスト
		 */
		private List<Key> ks = new ArrayList<Key>(1);
		/**
		 * 枝のリスト
		 */
		private List<Node> ns = new ArrayList<Node>(2);

		/**
		 * コンストラクタ
		 */
		public NodeActive(Key key, Node l, Node r) {
			ks.add(key);
			ns.add(l);
			ns.add(r);
		}

		@Override
		public List<Key> ks() {
			return ks;
		}

		@Override
		public List<Node> ns() {
			return ns;
		}

		/**
		 * アクティブなノードを内部ノードに変換する
		 */
		@Override
		public Node deactivate() {
			return new NodeInterior(ks.get(0), ns.get(0), ns.get(1));
		}
	}

	/**
	 * 内部ノードの型
	 */
	private class NodeInterior extends Node {
		/**
		 * キーのリスト
		 */
		private List<Key> ks = new ArrayList<Key>(m);
		/**
		 * 枝のリスト
		 */
		private List<Node> ns = new ArrayList<Node>(m + 1);

		/**
		 * コンストラクタ
		 */
		public NodeInterior() {}

		/**
		 * コンストラクタ
		 */
		public NodeInterior(Key key, Node l, Node r) {
			ks.add(key);
			ns.add(l);
			ns.add(r);
		}

		@Override
		public List<Key> ks() {
			return ks;
		}

		@Override
		public List<Node> ns() {
			return ns;
		}

		/**
		 * 枝が一本の余分なノードを切り詰める
		 */
		@Override
		public Node trim() {
			return ns.size() == 1 ? ns.get(0) : this;
		}

        //=====================================================================
        // 内部ノードでの挿入
        //=====================================================================

		/**
		 * 木 this にキー key で value を挿入する
		 */
		@Override
		public Node insert(Key key, Value value) {
			int i;
			for(i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					ns.set(i, ns.get(i).insert(key, value));
					return balance(i);
				} else if(compare == 0) {
					ns.set(i + 1, ns.get(i + 1).insert(key, value));
					return balance(i + 1);
				}
			}
			ns.set(i, ns.get(i).insert(key, value));
			return balance(i);
		}

		/**
		 * 挿入時のバランス調整
		 */
		public Node balance(int i) {
			Node ni = ns.get(i);
			if(!active(ni)) {
				return this;
			}
			ks.add(i, ni.ks().get(0));
			ns.set(i, ni.ns().get(1));
			ns.add(i, ni.ns().get(0));
			return ks.size() < m ? this : split();
		}

		/**
		 * 要素数がmのノードを分割してアクティブなノードに変換する
		 */
		public Node split() {
			int j = hm;
			int i = j -1;
			NodeInterior l = this;
			NodeInterior r = new NodeInterior();
			r.ks.addAll(l.ks.subList(j, m));
			r.ns.addAll(l.ns.subList(j, m + 1));
			l.ks.subList(j, m).clear();
			l.ns.subList(j, m + 1).clear();
			return new NodeActive(l.ks.remove(i), l, r);
		}

        //=====================================================================
        // 内部ノードでの削除
        //=====================================================================

		/**
		 * 木 this からキー key のノードを削除する
		 */
		@Override
		public void delete(Key key) {
			int i;
			for(i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					ns.get(i).delete(key);
					ns.get(i).balanceLeft(this, i);
					return;
				} else if(compare == 0) {
					ks.set(i, ns.get(i + 1).deleteMin());
					ns.get(i + 1).balanceRight(this, i + 1);
					return;
				}
			}
			ns.get(i).delete(key);
			ns.get(i).balanceRight(this, i);
		}

		/**
		 * 部分木 this の最小値キーを削除する
		 * @return 部分木 this の新たな最小値キー
		 */
		@Override
		public Key deleteMin() {
			Key nmin = ns.get(0).deleteMin();
			Key spare = ks.get(0);
			ns.get(0).balanceLeft(this, 0);
			return nmin != null ? nmin : spare;
		}

		/**
		 * 左部分木での削除時のバランス調整
		 */
		@Override
		public void balanceLeft(NodeInterior t, int i) {
			NodeInterior ni = this;
			if(ni.ns.size() >= hm) {
				return;
			}
			//以下、ni がアクティブな場合
			int j = i + 1;
			Key key = t.ks.get(i);
			NodeInterior nj = (NodeInterior) t.ns.get(j);
			if(nj.ns.size() == hm) {
				ni.ks.add(key);
				ni.ks.addAll(nj.ks);
				ni.ns.addAll(nj.ns);
				t.ks.remove(i);
				t.ns.remove(j);
			} else {
				t.ks.set(i, moveRL(key, ni, nj));
			}
		}

		/**
		 * 右部分木での削除時のバランス調整
		 */
		@Override
		public void balanceRight(NodeInterior t, int j) {
			NodeInterior nj = this;
			if(nj.ns.size() >= hm) {
				return;
			}
			// 以下、nj がアクティブの場合
			int i = j - 1;
			Key key = t.ks.get(i);
			NodeInterior ni = (NodeInterior) t.ns.get(i);
			if(ni.ns.size() == hm) {
				ni.ks.add(key);
				ni.ks.addAll(nj.ks);
				ni.ns.addAll(nj.ns);
				t.ks.remove(i);
				t.ns.remove(j);
			} else {
				t.ks.set(i, moveLR(key, ni, nj));
			}
		}

		/**
		 * 余裕のある右ノードから枝を1本分けてもらう
		 */
		public Key moveRL(Key key, NodeInterior l, NodeInterior r) {
			l.ks.add(key);
			l.ns.add(r.ns.remove(0));
			return r.ks.remove(0);
		}

		/**
		 * 余裕のある左ノードから枝を1本分けてもらう
		 */
		public Key moveLR(Key key, NodeInterior l, NodeInterior r) {
			int j = l.ks.size();
			int i = j - 1;
			r.ks.add(0, key);
			r.ns.add(0, l.ns.remove(j));
			return l.ks.remove(i);
		}
	}

	/**
	 * 最下層のノードの型
	 */
	public class NodeBottom extends Node {
		/**
		 * キーのリスト
		 */
		private List<Key> ks = new ArrayList<Key>(m);
		/**
		 * 値のリスト
		 */
		private List<Value> vs = new ArrayList<Value>(m);
		/**
		 * 右隣の最下層のノード
		 */
		private NodeBottom next = nil;

		/**
		 * コンストラクタ
		 */
		public NodeBottom() {}

		/**
		 * コンストラクタ
		 */
		public NodeBottom(Key key, Value value) {
			ks.add(key);
			vs.add(value);
		}

		@Override
		public List<Key> ks() {
			return ks;
		}

		/**
		 * キーも枝もないノードを空の木(nil)に変換する
		 */
		@Override
		public Node trim() {
			return ks.size() == 0 ? nil : this;
		}

        //=====================================================================
        // 最下層のノードでの挿入
        //=====================================================================

		/**
		 * 木 this にキー key で値 value を挿入する
		 */
		@Override
		public Node insert(Key key, Value value) {
			if(this == nil) {
				return new NodeBottom(key, value);
			}
			int i;
			for(i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					return balance(i, key, value);
				} else if(compare == 0) {
					vs.set(i, value);
					return this;
				}
			}
			return balance(i, key, value);
		}

		/**
		 * 挿入時のバランス調整
		 */
		public Node balance(int i, Key key, Value value) {
			ks.add(i, key);
			vs.add(i, value);
			return ks.size() < m ? this : split();
		}

		/**
		 * 要素数が m のノードを分割してアクティブなノードに変換する
		 */
		public Node split() {
			int j = hm - 1;
			NodeBottom l = this;
			NodeBottom r = new NodeBottom();
			r.ks.addAll(l.ks.subList(j, m));
			r.vs.addAll(l.vs.subList(j, m));
			l.ks.subList(j, m).clear();
			l.vs.subList(j, m).clear();
			r.next = l.next;
			l.next = r;
			return new NodeActive(r.ks.get(0), l, r);
		}

        //=====================================================================
        // 最下層のノードでの削除
        //=====================================================================

		/**
		 * 木 this からキー key のノードを削除する
		 */
		@Override
		public void delete(Key key) {
			for(int i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					return;
				} else if(compare == 0) {
					ks.remove(i);
					vs.remove(i);
					return;
				}
			}
		}

		/**
		 * 部分木 this の最小値キーを削除する
		 * @return 部分木 this の新たな最小値キーを返す。空になったら null を返す
		 */
		@Override
		public Key deleteMin() {
			ks.remove(0);
			vs.remove(0);
			return !ks.isEmpty() ? ks.get(0) : null;
		}

		/**
		 * 左部分木での削除時のバランス調整
		 */
		@Override
		public void balanceLeft(NodeInterior t, int i) {
			NodeBottom ni = this;
			if(ni.ks.size() >= hm - 1) {
				return;
			}
			int j = i + 1;
			NodeBottom nj = (NodeBottom) t.ns.get(j);
			if(nj.ks.size() == hm - 1) {
				ni.ks.addAll(nj.ks);
				ni.vs.addAll(nj.vs);
				t.ks.remove(i);
				t.ns.remove(j);
				ni.next = nj.next;
			} else {
				t.ks.set(i, moveRL(ni, nj));
			}
		}

		/**
		 * 右部分木での削除時のバランス調整
		 */
		@Override
		public void balanceRight(NodeInterior t, int j) {
			NodeBottom nj = this;
			if(nj.ks.size() >= hm - 1) {
				return;
			}
			int i = j - 1;
			NodeBottom ni = (NodeBottom) t.ns.get(i);
			if(ni.ks.size() == hm - 1) {
				ni.ks.addAll(nj.ks);
				ni.vs.addAll(nj.vs);
				t.ks.remove(i);
				t.ns.remove(j);
				ni.next = nj.next;
			} else {
				t.ks.set(i, moveLR(ni, nj));
			}
		}

		/**
		 * 余裕のある右ノードから枝を1本分けてもらう
		 */
		public Key moveRL(NodeBottom l, NodeBottom r) {
			l.ks.add(r.ks.remove(0));
			l.vs.add(r.vs.remove(0));
			return r.ks.get(0);
		}

		/**
		 * 余裕のある左ノードから枝を1本分けてもらう
		 */
		public Key moveLR(NodeBottom l, NodeBottom r) {
			int i = l.ks.size() - 1;
			r.ks.add(0, l.ks.remove(i));
			r.vs.add(0, l.vs.remove(i));
			return r.ks.get(0);
		}

		public List<Value> getVs() {
			return this.vs;
		}

		public NodeBottom getNext() {
			return this.next;
		}
	}

	/**
	 * 木 root にキー key で値 x を挿入する
	 */
	public void insert(Key key, Value value) {
		root = root.insert(key, value).deactivate();
	}

	/**
	 * キー key の値 value を更新する
	 */
	public void update(Key key, Value value) {
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					i++;
					break;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) == 0) {
				u.vs.set(i, value);
				return;
			}
		}
	}

	/**
	 * 木 root からキー key のノードを削除する
	 */
	public void delete(Key key) {
		root.delete(key);
		root = root.trim();
	}

	/**
	 * キーの検索
	 * @return ヒットすれば true、しなければ false
	 */
	public boolean member(Key key) {
		if(root == nil) {
			return false;
		}
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					return true;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * キーから値を得る。
	 * @return キーから得た値。ヒットしない場合 null を返す
	 */
	public Value lookup(Key key) {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					i++;
					break;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) == 0) {
				return u.vs.get(i);
			}
		}
		return null;
	}

	/**
	 * 最小値の入ったノードを返す
	 */
	public NodeBottom minNode() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		NodeBottom nb = (NodeBottom) t;
		return nb;
	}

	/**
	 * 範囲検索を行う。
	 */
	public ArrayList<Value> range(Key key, int range) {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					i++;
					break;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		ArrayList<Value> result = new ArrayList<Value>();
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) <= 0) {
				while(i < u.ks.size() && 0 < range) {
					result.add(u.vs.get(i));
					range--;
					i++;
					if(i == u.ks.size() && 0 < range) {
						u = u.next;
						if(u == nil) {
							break;
						}
						i = 0;
					} else if(range == 0) {
						break;
					}
				}
				break;
			}
		}
		return result;
	}

	/**
	 * マップが空かどうか
	 * @return マップが空なら true、空でないなら false
	 */
	public boolean isEmpty() {
		return root == nil;
	}

	/**
	 * マップを空にする
	 */
	public void clear() {
		root = nil;
	}

	/**
	 * B+木のキーリストを返す
	 * @return B+木のキーのリスト
	 */
	public ArrayList<Key> keys() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		NodeBottom u = (NodeBottom) t;
		ArrayList<Key> al = new ArrayList<Key>();
		while(u != nil) {
			al.addAll(u.ks);
			u = u.next;
		}
		return al;
	}

	/**
	 * B+木の値のリストを返す
	 * @return B+木の値の値のリスト
	 */
	public ArrayList<Value> values() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		NodeBottom u = (NodeBottom) t;
		ArrayList<Value> al = new ArrayList<Value>();
		while(u != nil) {
			al.addAll(u.vs);
			u = u.next;
		}
		return al;
	}

	/**
	 * マップのサイズを返す
	 * @return マップのサイズ
	 */
	public int size() {
		return keys().size();
	}

	/**
	 * キーの最小値を返す
	 * @return キーの最小値
	 */
	public Key min() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		return t.ks().get(0);
	}

	/**
	 * キーの最大値を返す
	 * @return キーの最大値
	 */
	public Key max() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(t.ns().size() - 1);
		}
		return t.ks().get(t.ks().size() - 1);
	}

    //=========================================================================
    // デバッグ用ルーチン
    //=========================================================================

	/**
	 * B+木をグラフ文字列に変換する
	 */
    public String toString() {
        return toGraph("", root).replaceAll("\\s+$", "");
    }

    private static final String nl = System.getProperty("line.separator");

    private String toGraph(String head, Node t) {
        if(t == nil) {
        	return "";
        }
        String graph = "";
        if(bottom(t)) {
            graph += head + t.ks() + nl;
        } else {
            int i = t.ns().size();
            graph += toGraph(head + "    ", t.ns().get(--i));
            graph += head + "∧" + nl;
            do {
                graph += head + t.ks().get(--i) + nl;
                if (i == 0) {
                	graph += head + "∨" + nl;
                }
                graph += toGraph(head + "    ", t.ns().get(i));
            } while (i > 0);
        }
        return graph;
    }

    /**
     * nilのゲッター
     */
	public NodeBottom getNil() {
		return this.nil;
	}
}
