package cs224n.assignment;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs224n.ling.Tree;
import cs224n.ling.Trees;
import cs224n.ling.Trees.MarkovizationAnnotationStripper;
import cs224n.util.Filter;

/**
 * Class which contains code for annotating and binarizing trees for
 * the parser's use, and debinarizing and unannotating them for
 * scoring.
 */
public class TreeAnnotations {

	public static Tree<String> annotateTree(Tree<String> unAnnotatedTree) {

		verticalMarkovAnnotate(unAnnotatedTree, null);
		Tree<String> binarizedTree = binarizeTree(unAnnotatedTree);
		horizontolMarkovAnnotate(binarizedTree, 2);
		// System.out.println("Original Tree");
		// System.out.println(Trees.PennTreeRenderer.render(unAnnotatedTree));
		// System.out.println("Annotated Tree");
		// System.out.println(Trees.PennTreeRenderer.render(binarizedTree));

		return binarizedTree;
	}

	private static void verticalMarkovAnnotate(Tree<String> tree, String parent) {
		if (tree.isLeaf() || tree.isPreTerminal()) return;
		String label = tree.getLabel();
		for (Tree<String> child: tree.getChildren()) {
			verticalMarkovAnnotate(child, label);
		}
		if (parent != null) {
			tree.setLabel(label + "^" + parent);
		}
	}

	private static void horizontolMarkovAnnotate(Tree<String> tree, int order) {
		String label = tree.getLabel();
		if (tree.isLeaf()) 
			return;

		if (tree.getChildren().size() == 1) {
			horizontolMarkovAnnotate(tree.getChildren().get(0), order);
			return;
		}
		// otherwise, it's a binary-or-more local tree
		String[] labels = label.split("@|_|->");
		List<String> parentLabels = new ArrayList<String>();
		for (String l: labels) {
			if (!l.isEmpty()) parentLabels.add(l);
		}
		
		String prefix = "@"+ parentLabels.get(0) + "->";
		String postfix = "_" + tree.getChildren().get(0).getLabel();
		String newRightLabel;
		if (order == 1) {
			newRightLabel = prefix + postfix;
		} else {
			String secondParent = parentLabels.size() > 1 
				? "_" + parentLabels.get(1)
				: "";

			newRightLabel = prefix + secondParent + postfix;
		}
		tree.getChildren().get(1).setLabel(newRightLabel);
		horizontolMarkovAnnotate(tree.getChildren().get(0), order);
		horizontolMarkovAnnotate(tree.getChildren().get(1), order);
	}

	private static Tree<String> binarizeTree(Tree<String> tree) {
		String label = tree.getLabel();
		if (tree.isLeaf())
			return new Tree<String>(label);
		if (tree.getChildren().size() == 1) {
			return new Tree<String>( 
				label, 
				Collections.singletonList(binarizeTree(tree.getChildren().get(0)))
			);
		}
		// otherwise, it's a binary-or-more local tree, 
		// so decompose it into a sequence of binary and unary trees.
		String intermediateLabel = "@"+label+"->";
		Tree<String> intermediateTree =
				binarizeTreeHelper(tree, 0, intermediateLabel);
		return new Tree<String>(label, intermediateTree.getChildren());
	}

	private static Tree<String> binarizeTreeHelper(Tree<String> tree,
			int numChildrenGenerated, 
			String intermediateLabel) {
		Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
		List<Tree<String>> children = new ArrayList<Tree<String>>();
		children.add(binarizeTree(leftTree));
		if (numChildrenGenerated < tree.getChildren().size() - 1) {
			Tree<String> rightTree = 
					binarizeTreeHelper(tree, numChildrenGenerated + 1, 
							intermediateLabel + "_" + leftTree.getLabel());
			children.add(rightTree);
		}
		return new Tree<String>(intermediateLabel, children);
	} 

	public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {

		// Remove intermediate nodes (labels beginning with "@"
		// Remove all material on node labels which follow their base symbol
		// (cuts at the leftmost - or ^ character)
		// Examples: a node with label @NP->DT_JJ will be spliced out, 
		// and a node with label NP^S will be reduced to NP

		Tree<String> debinarizedTree =
				Trees.spliceNodes(annotatedTree, new Filter<String>() {
					public boolean accept(String s) {
						return s.startsWith("@");
					}
				});
		Tree<String> unAnnotatedTree = 
				(new Trees.FunctionNodeStripper()).transformTree(debinarizedTree);
    Tree<String> unMarkovizedTree =
        (new Trees.MarkovizationAnnotationStripper()).transformTree(unAnnotatedTree);
		return unMarkovizedTree;
	}
}
