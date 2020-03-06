package minimax;

import java.util.HashMap;

import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;

public class Trees {
	public Trees(StateView state) {
		this.state = state;
	}
	private StateView state;
	private boolean hasTrees = false;
	private HashMap<Integer, Tree> trees = new HashMap<Integer, Tree>(); 
	public HashMap<Integer, Tree> getTrees(){
		return trees;
	}
	public boolean hasTrees() {
		return hasTrees;
	}
	public HashMap<Integer, Tree> createTreeTable(){
		//runs through all trees
		for (ResourceView tree: state.getAllResourceNodes()) {
			//if they're not in the hash table, add them
			Tree tree2 = trees.get(bijectiveAlg(tree.getXPosition(), tree.getYPosition()));
			if (tree2 == null) {
				hasTrees = true;
				tree2 = new Tree(tree);
			   findTrees(tree2);
			}
		}
		return trees;
	}
	
	    //this functions assuming only two trees are touching at a time
	    public void findTrees(Tree to){	
	    	to.visited = true;
	    	Tree neighbor = findNext(to);
	    	if (neighbor != null) {
	    	//length = child's length 
	    		findTrees(neighbor);
	    		to.length = neighbor.length + 1;
	    	}
	    }
	    	//returns the next neighboring tree and adds to hash table if it's not already there
	    	public Tree findNext(Tree tree){
	        	for (int i = - 1; i < 2; i++) {
	        		for (int j = -1; j < 2; j++) {
	        			Integer neighbor = state.resourceAt(tree.tree.getXPosition() + i, tree.tree.getYPosition() + j);
	        			if (neighbor != null) {
	        				ResourceView tree2 = state.getResourceNode(neighbor);
	        				int hashCode = bijectiveAlg(tree2.getXPosition(), tree2.getYPosition()); 
	        				if (!trees.containsKey(hashCode)){ // && !trees.get(hashCode).visited
	        					Tree newTree = new Tree(tree2, 1);
	        					trees.put(hashCode, newTree);
	        					return newTree;
	        				}
	        			}
	        		}
	        	}
	        	return null;
	    	}
	    	
	    	public static int bijectiveAlg(int x, int y) {
				 int tmp = y +  (x + 1) / 2;
		         return x +  tmp * tmp;
		    }
	    //might want an adjacency list
	    public class Tree {
	    	public Integer length = 1;
	    	public ResourceView tree;
	    	public boolean visited = false;
	    	public Tree (ResourceView tree) {
	    		this.tree = tree;
	    	}
	    	public Tree (ResourceView tree, Integer leng) {
	    		this.tree = tree;
	    		this.length = leng;
	    	}
	    	@Override //bijective algorithm to get hash code in constant time from coordinates
	    	public int hashCode() {
	             return bijectiveAlg(tree.getXPosition(), tree.getYPosition());
	    	}
	    	@Override
	    	public boolean equals(Object tree2) {
	    		return ((Tree)tree2).tree.getXPosition() == tree.getXPosition() && ((Tree)tree2).tree.getYPosition() == tree.getYPosition();
	    	}
	    }
	    
}
