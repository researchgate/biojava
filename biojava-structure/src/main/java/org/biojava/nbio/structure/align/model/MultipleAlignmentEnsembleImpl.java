package org.biojava.nbio.structure.align.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Calc;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.jama.Matrix;

/**
 * A general implementation of a {@link MultipleAlignmentEnsemble}.
 * 
 * @author Aleix Lafita
 *
 */
public class MultipleAlignmentEnsembleImpl implements MultipleAlignmentEnsemble, Serializable{

	private static final long serialVersionUID = -5732485866623431898L;
	
	//Creation Properties
	String algorithmName;
	String version;
	long ioTime;
	long id;
	long calculationTime;							//running time of the algorithm
	
	//Structure Identifiers
	private List<String> structureNames;  			//names of the structures in PDB or SCOP format
	private List<Atom[]> atomArrays;      			//arrays of atoms for every structure in the alignment
	private List<Matrix> distanceMatrix; 			//A list of n (l*l)-matrices that store the distance between every pair of residues for every protein
														//n=nr. structures; l=length of the protein
	private List<MultipleAlignment> multipleAlignments;
	
	/**
	 * Default Constructor. Empty ensemble, no structures assigned.
	 * @return MultipleAlignmentEnsemble an empty MultipleAlignmentEnsemble instance.
	 */
	public MultipleAlignmentEnsembleImpl(){
		
		algorithmName = null;
		version = "1.0";
		ioTime = System.currentTimeMillis();
		id = 0;
		calculationTime = 0;
		
		structureNames = null;
		atomArrays = null;
		distanceMatrix = null;
		multipleAlignments = null;
	}
	
	/**
	 * Constructor using structure identifiers.
	 * @param structureNames List of Structure names, that can be parsed by AtomCache.
	 * @return MultipleAlignmentEnsemble a MultipleAlignmentEnsemble instance with the structures.
	 */
	public MultipleAlignmentEnsembleImpl(List<String> structureNames){
		
		this();
		setStructureNames(structureNames);
	}
	
	/**
	 * Copy constructor.
	 * @param e MultipleAlignmentEnsembleImpl to copy.
	 * @return MultipleAlignmentEnsembleImpl identical copy of the input MultipleAlignmentEnsembleImpl.
	 */
	public MultipleAlignmentEnsembleImpl(MultipleAlignmentEnsembleImpl e){
		
		algorithmName = e.algorithmName;
		version = e.version;
		ioTime = e.ioTime;
		id = e.id;
		calculationTime = e.calculationTime;
		
		atomArrays = null;
		if (e.atomArrays != null){
			//Make a deep copy of everything
			atomArrays = new ArrayList<Atom[]>();
			for (Atom[] array:e.atomArrays){
				Atom[] newArray = new Atom[array.length];
				for (int i=0; i<array.length; i++){
					newArray[i] = (Atom) array[i].clone();
				}
				atomArrays.add(newArray);
			}
		}
		
		distanceMatrix = null;
		if (e.distanceMatrix!=null){
			//Make a deep copy of everything
			distanceMatrix = new ArrayList<Matrix>();
			for (Matrix mat:e.distanceMatrix){
				distanceMatrix.add((Matrix) mat.clone());
			}
		}
		
		multipleAlignments = null;
		if (e.multipleAlignments!=null){
			//Make a deep copy of everything
			multipleAlignments = new ArrayList<MultipleAlignment>();
			for (MultipleAlignment msa:e.multipleAlignments){
				MultipleAlignment newMSA = (MultipleAlignment) msa.clone();
				newMSA.setParent(this);  //This automatically adds the newMSA to the multipleAlignments list
			}
		}
		
		structureNames = new ArrayList<String>(e.structureNames);
	}
	
	@Override
	public Object clone() {
		return new MultipleAlignmentEnsembleImpl(this);
	}
	
	@Override
	public String getAlgorithmName() {
		return algorithmName;
	}

	@Override
	public void setAlgorithmName(String algorithmName) {
		this.algorithmName = algorithmName;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public long getIoTime() {
		return ioTime;
	}

	@Override
	public long getCalculationTime() {
		return calculationTime;
	}

	@Override
	public void setCalculationTime(long calculationTime) {
		this.calculationTime = calculationTime;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public List<String> getStructureNames() {
		return structureNames;
	}

	@Override
	public void setStructureNames(List<String> structureNames) {
		this.structureNames = structureNames;
	}

	@Override
	public List<Atom[]> getAtomArrays() throws StructureAlignmentException {
		if (atomArrays == null) throw new StructureAlignmentException("Empty MultipleAlignmentEnsemble: updateAtomArrays() first");
		return atomArrays;
	}

	@Override
	public void setAtomArrays(List<Atom[]> atomArrays) {
		this.atomArrays = atomArrays;
		//If the atomArrays are changed the structure identifiers must be also changed.
		structureNames = new ArrayList<String>();
		for (int i=0; i<atomArrays.size(); i++)
			structureNames.add(atomArrays.get(i)[0].getGroup().getChain().getParent().getIdentifier());
	}
	
	@Override
	public void updateAtomArrays() throws StructureAlignmentException, IOException, StructureException{
		AtomCache cache = new AtomCache();
		atomArrays = new ArrayList<Atom[]>();
		for (int s=0; s<size(); s++){
			Atom[] array = cache.getAtoms(structureNames.get(s));
			atomArrays.add(array);
		}
	}

	@Override
	public int getAlignmentNum() {
		return multipleAlignments.size();
	}

	@Override
	public List<Matrix> getDistanceMatrix() throws StructureAlignmentException {
		if (distanceMatrix == null) updateDistanceMatrix();
		return distanceMatrix;
	}

	@Override
	public void updateDistanceMatrix() throws StructureAlignmentException {
		
		//Reset the distance Matrix variable
		distanceMatrix = new ArrayList<Matrix>();
		
		for (int s=0; s<size(); s++){
			int n = atomArrays.get(s).length;  //length of the structure
			Matrix distMat = new Matrix(n,n);
			
			//Calculate all distances between every pair of atoms and set the entries
			for (int a1=0; a1<n; a1++){
				for (int a2=0; a2<n; a2++){
					double dist = Calc.getDistance(atomArrays.get(s)[a1], atomArrays.get(s)[a2]);
					distMat.set(a1, a2, dist);
				}
			}
			distanceMatrix.add(distMat);
		}
	}

	@Override
	public List<MultipleAlignment> getMultipleAlignments() {
		if (multipleAlignments == null) multipleAlignments = new ArrayList<MultipleAlignment>();
		return multipleAlignments;
	}

	@Override
	public void setMultipleAlignments(List<MultipleAlignment> multipleAlignments) {
		this.multipleAlignments = multipleAlignments;
	}

	@Override
	public MultipleAlignment getOptimalMultipleAlignment() throws StructureAlignmentException {
		if (getAlignmentNum() == 0) throw new StructureAlignmentException("Empty MultipleAlignmentEnsemble: getAlignmentNum() == 0");
		else return multipleAlignments.get(0);
	}

	@Override
	public int size() throws StructureAlignmentException {
		if (structureNames == null) throw new StructureAlignmentException("Empty MultipleAlignmentEnsemble: structureNames == null");
		else return structureNames.size();
	}

}
