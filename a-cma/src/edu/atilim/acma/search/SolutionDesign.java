package edu.atilim.acma.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.atilim.acma.RunConfig;
import edu.atilim.acma.design.Design;
import edu.atilim.acma.metrics.MetricCalculator;
import edu.atilim.acma.transition.TransitionManager;
import edu.atilim.acma.transition.actions.Action;
import edu.atilim.acma.util.ACMAUtil;
import edu.atilim.acma.util.Log;

public class SolutionDesign implements Iterable<SolutionDesign>, Comparable<SolutionDesign> {
	private Design design;
	private RunConfig config;
	private List<Action> actions;
	
	private double score = Double.NaN;
	
	public Design getDesign() {
		return design;
	}

	public RunConfig getConfig() {
		return config;
	}
	
	public double getScore() {
		if (Double.isNaN(score)) {
			score = MetricCalculator.normalize(MetricCalculator.calculate(design, config), config);
		}
		return score;
	}
	
	public SolutionDesign getBestNeighbor() {
		int numparts = Runtime.getRuntime().availableProcessors();
		SolutionDesign best = this;
		
		if (numparts == 1) {
			for (SolutionDesign sd : this) {
				if (sd.isBetterThan(best))
					best = sd;
			}
			return best;
		}
		
		List<Action> actions = getAllActions();
		
		int perthread = actions.size() / numparts;
		
		List<BestDesignFinder> bdf = new ArrayList<SolutionDesign.BestDesignFinder>(numparts);
		for (int i = 0; i < 2; i++) {
			bdf.add(new BestDesignFinder(actions, i * perthread, perthread));
		}
		
		try {
			// Submit to thread pool
			List<Future<SolutionDesign>> futures = threadPool.invokeAll(bdf);
			
			// Remainder
			for (int i = perthread * numparts; i < actions.size(); i++) {
				SolutionDesign cur = apply(actions.get(i));
				if (cur.isBetterThan(best)) best = cur;
			}
			
			for (Future<SolutionDesign> f : futures) {
				SolutionDesign cur = f.get();
				if (cur.isBetterThan(best))
					best = cur;
			}
		} catch (Exception e) {
			Log.severe("Exception in parallel design extraction: %s", e.getMessage());
			e.printStackTrace();
			return this;
		}
		
		return best;
	}
	
	public SolutionDesign getRandomNeighbor() {
		List<Action> actions = getAllActions();
		if (actions.isEmpty()) return this;
		return apply(actions.get(ACMAUtil.RANDOM.nextInt(actions.size())));
	}
	
	public SolutionDesign getRandomNeighbor(int depth) {
		SolutionDesign random = this;
		for (int i = 0; i < depth; i++)
			random = random.getRandomNeighbor();
		return random;
	}
	
	public List<Action> getAllActions() {
		if (actions == null) {
			actions = new ArrayList<Action>(TransitionManager.getPossibleActions(design, config));
		}
		return actions;
	}
	
	public SolutionDesign(Design design, RunConfig config) {
		this.design = design;
		this.config = config;
	}
	
	public boolean isBetterThan(SolutionDesign other) {
		return compareTo(other) > 0;
	}
	
	@Override
	public int compareTo(SolutionDesign o) {
		return Double.compare(compareScoreTo(o), 0.0);
	}
	
	public double compareScoreTo(SolutionDesign o) {
		return o.getScore() - getScore();
	}

	@Override
	public Iterator<SolutionDesign> iterator() {
		return new Iter();
	}
	
	private SolutionDesign apply(Action action) {
		Design copyDesign = design.copy();
		action.perform(copyDesign);
		copyDesign.logModification(action.toString());
		return new SolutionDesign(copyDesign, config);
	}
	
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	
	private class BestDesignFinder implements Callable<SolutionDesign> {
		private List<Action> actions;
		private int offset;
		private int count;

		private BestDesignFinder(List<Action> actions, int offset, int count) {
			this.actions = actions;
			this.offset = offset;
			this.count = count;
		}

		@Override
		public SolutionDesign call() throws Exception {
			SolutionDesign best = SolutionDesign.this;
			for (int i = offset; i < offset + count; i++) {
				Action action = actions.get(i);
				SolutionDesign newDesign = apply(action);
				if (newDesign.isBetterThan(best))
					best = newDesign;
			}
			
			return best;
		}
		
	}
	
	private class Iter implements Iterator<SolutionDesign> {
		private Iterator<Action> innerIterator;
		
		private Iter() {
			innerIterator = getAllActions().iterator();
		}
		
		@Override
		public boolean hasNext() {
			return innerIterator.hasNext();
		}

		@Override
		public SolutionDesign next() {
			return apply(innerIterator.next());
		}

		@Override
		public void remove() {
			innerIterator.remove();
		}
	}
}
