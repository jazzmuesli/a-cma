package org.pavelreich.saaremaa;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import edu.atilim.acma.RunConfig;
import edu.atilim.acma.design.Design;
import edu.atilim.acma.design.Method;
import edu.atilim.acma.design.Type;
import edu.atilim.acma.design.io.DesignReader;
import edu.atilim.acma.search.AbstractAlgorithm;
import edu.atilim.acma.search.AlgorithmObserver;
import edu.atilim.acma.search.SimAnnAlgorithm;
import edu.atilim.acma.search.SolutionDesign;
import edu.atilim.acma.ui.ConfigManager;
import me.tongfei.progressbar.ProgressBar;

public class StandaloneRefactorTool {
	
	public static void main(String[] args) throws InterruptedException {
		File file = new File("/Users/preich/Documents/git/untestable-code1/target/classes");
		DesignReader loader = new DesignReader(file.getAbsolutePath());
		Design design = loader.read();
		int mi = 1000;
		RunConfig runConfig = ConfigManager.getRunConfig("Default");
		final ProgressBar pb = new ProgressBar("refactor", mi);
		final CountDownLatch latch = new CountDownLatch(1);
		AlgorithmObserver observer = new AlgorithmObserver() {
			
			@Override
			public void onUpdateItems(AbstractAlgorithm algorithm, SolutionDesign current, SolutionDesign best,
					int updateType) {
				System.out.println("onUpdateItems: current: "+ current.getScore() + ", best: " + best.getScore());
				
			}
			
			@Override
			public void onStep(AbstractAlgorithm algorithm, int step) {
				pb.step();
			}
			
			@Override
			public void onStart(AbstractAlgorithm algorithm, SolutionDesign initial) {
				printDesign(initial);
				
			}
			
			@Override
			public void onLog(AbstractAlgorithm algorithm, String log) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onFinish(AbstractAlgorithm algorithm, SolutionDesign last) {
				for (String m : last.getDesign().getModifications()) {
					System.out.println(m);
				}
				printDesign(last);
				pb.close();
				latch.countDown();
			}
			
			@Override
			public void onExpansion(AbstractAlgorithm algorithm, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAdvance(AbstractAlgorithm algorithm, int current, int total) {
				// TODO Auto-generated method stub
				
			}
		};
		SolutionDesign initialDesign = new SolutionDesign(design, runConfig);
		//RandomSearchAlgorithm algo = new RandomSearchAlgorithm(initialDesign, observer, mi);
		SimAnnAlgorithm algo = new SimAnnAlgorithm(initialDesign, observer, 30.0, mi);
		algo.start();
		latch.await();
		System.exit(0);
	}

	protected static void printDesign(SolutionDesign last) {
		List<Type> types = last.getDesign().getTypes();
		for (Type type : types) {
			for (Method method : type.getMethods()) {
				System.out.println(method.getFlags() + " " + type.getName()+"::"  + method.getSignature()+":" + method.getReturnType()+ ", loc=" + method.getLoc());	
			}
			
		}
	}

}
