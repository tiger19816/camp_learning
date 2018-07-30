package bpTree;

import java.util.ArrayList;
import java.util.List;

public class BPMap<Key extends Comparable<? super Key>, Value> {

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
	private abstract class Node {
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
	private final NodeBottom nil = nwe NodeBottom();
	/**
	 * B+木の根(nilは空の木)
	 */
	private Node root = nil;

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
		 * @param key
		 * @param l
		 * @param r
		 */
		public NodeActive(Key key, Node l, Node r) {
			ks.add(key);
			ns.add(l);
			ns.add(r);
		}

		public List<Key> ks() {
			return ks;
		}

		public List<Node> ns() {
			return ns;
		}

		/**
		 * アクティブなノードを内部ノードに変換する
		 */
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

		public List<Key> ks() {
			return ks;
		}

		public List<Node> ns() {
			return ns;
		}

		/**
		 * 枝が一本の余分なノードを切り詰める
		 */
		public Node trim() {
			return ns.size() == 1 ? ns.get(0) : this;
		}

		/**
		 * 木 this にキー key で value を挿入する
		 */
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
			ns.set(i, ni.ns().get(0));
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

		/**
		 * 木 this からキー key のノードを削除する
		 */
		public void delste(Key key) {
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
				}
				ns.get(i).delete(key);
				ns.get(i).balanceRight(this, i);
			}
		}

		/**
		 * 部分木 this の最小値キーを削除する
		 * @return 部分木 this の当たらな最小値キー
		 */
		public Key deleteMin() {
			Key nmin = ns.get(0).deleteMin();
			Key spare = ks.get(0);
			ns.get(0).balanceLeft(this, 0);
			return nmin != null ? nmin : spare;
		}

		/**
		 * 左部分木での削除時のバランス調整
		 */
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
		public void balanceRight(NodeInterior t, int j) {
			NodeInterior nj = this;
			if(nj.ns.size() >= hm) {
				return;
			}
			// 以下、nj がアクティブの場合
			int i = j - 1;
			Key key = t.ks.get(i);
			NodeInterior ni = (NodeInterior) t.ns.get(j);
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


}
