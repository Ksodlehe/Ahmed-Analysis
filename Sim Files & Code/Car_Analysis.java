import java.util.*;

import java.io.*;

import star.base.neo.ClientServerObject;
import star.base.neo.ClientServerObjectGroup;
import star.base.neo.NamedObject;
import star.common.*;
import star.common.graph.DataSet;
import star.meshing.AutoMeshOperation;
import star.meshing.MeshOperationManager;
import star.meshing.SubtractPartsOperation;
import java.util.concurrent.TimeUnit;

public class Car_Analysis extends StarMacro {
  private File out = null;
  private double meshTime = 0;

  public void execute() {
    execute0();
  }
  private void execute0() {
    Simulation sim = getActiveSimulation();

    Units m = (Units) sim.getUnitsManager().getObject("m");
    Units mm = (Units) sim.getUnitsManager().getObject("mm");

    // Set maximum steps
    setMaxSteps(1000);

    // Set units
    setTunnelUnits(m, m, m, m);
    setMeshingUnits(mm, mm, mm, mm, mm);

    // Set parameters
    setMeshingScalars(5,20, 3, 100, 20, 20);
    setTunnelOffsets(9, 4, 2, 3);

    // run a sim
    runSim();
  }

 
  /**
   * Run a singular simulation
   * @param comment A comment to be included
   */
  private void runSim(String comment){
    String error = "N/A";
    Simulation sim = getActiveSimulation();
    try{
      sim.println("|=====--- " + parametersToString() + "---=====|");

      // Create operations
      SubtractPartsOperation subtract= 
        (SubtractPartsOperation) sim.get(MeshOperationManager.class).getObject("Subtract");
      AutoMeshOperation autoMesh = 
        (AutoMeshOperation) sim.get(MeshOperationManager.class).getObject("Automated Mesh");
      Solution solution = sim.getSolution();

      // Time taken to mesh part
      double start = System.currentTimeMillis();
      subtract.execute();
      autoMesh.execute();
      double end = System.currentTimeMillis();

      meshTime = (double) (end - start) / 60000;

      // Clear, initialize and run solution
      solution.clearSolution(Solution.Clear.History, Solution.Clear.Fields, Solution.Clear.LagrangianDem);
      solution.initializeSolution();

      sim.getSimulationIterator().run();

      sim.println("Running finished");
    }
    catch (Exception e){
      // Create error message
      error = e.getMessage().replaceAll("\n", "");
    }
    finally{
      // Write data to output file
      writeData(error, comment);

      try{
        sim.println("Next sim will start in 10 seconds...");
        TimeUnit.SECONDS.sleep(5);
        sim.print("Next sim will start in 5...");
        TimeUnit.SECONDS.sleep(1);
        sim.print("4...");
        TimeUnit.SECONDS.sleep(1);
        sim.print("3...");
        TimeUnit.SECONDS.sleep(1);
        sim.print("2...");
        TimeUnit.SECONDS.sleep(1);
        sim.print("1...\n");
        TimeUnit.SECONDS.sleep(1);
        sim.println("Starting next sim...");
      }
      catch(Exception e){
        // DO NOTHING
      }
    }
  }
  /**
   * Run a singular simulation without adding a comment
   */
  private void runSim(){
    runSim("N/A");
  }


  /**
   * Create a file if it does not exist
   * @return The created file
   */
  private File outputFile(String path){
    File file = new File(path);

    if(file.exists())
      return file;
    
    Simulation sim = getActiveSimulation();
    // Create file and write header row > Parameters - Reports - Residuals
    try {
      file.createNewFile();
      sim.println("File created at " + path);

      BufferedWriter bw = new BufferedWriter(new FileWriter(file,true));

      sim.println("Writing spreadsheet header");
      String header = "";

      // Get all parameters
      sim.println("|--Getting parameters...");
      for(NamedObject param : getParameters()){
        header += param.getDisplayName() + ", ";
      }

      // Write the coefficient data to file
      sim.println("|--Writing coefficients...");
      for(DataSet report : getCoefficients()){
        header += report.getName().replaceAll("Monitor", "") + "[" + report.getYUnits().toString() + "], ";
      }

      // Get all names + units for the reports
      sim.println("|--Getting reports...");
      for(DataSet report : getReports()){
        header += report.getName().replaceAll("Monitor", "") + "[" + report.getYUnits().toString() + "], ";
      }
      
      // Add mesh time
      header += "Total Meshing Time [min], ";

      // Add meshing data and iteration step
      header += "Cell Count, Face Count, Vertex Count, Iteration, ";

      // Get all residuals
      sim.println("|--Getting residuals...");
      for(DataSet residual : getResiduals()){
        header += residual.getName() + ", ";
      }
      
      // Remove final ', ' from header
      //header = header.substring(0, header.length() - 2);

      header += "Error, Comment";
      
      // Print and write
      bw.write(header + "\n");
      sim.println("Header written: "+ header);

      bw.close();
    } catch (IOException e) {
      sim.print(e.getLocalizedMessage());
    }

    return file;
  }
  /**
   * Write the data obtained from the simulation to the
   * 'out' file
   */
  private void writeData(String error, String comment){
    Simulation sim = getActiveSimulation();
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(out, true));

      // Write the parameter data to file
      sim.println("|--Writing parameters...");
      
      String data = "";
      for(NamedObject param : getParameters()){
        ScalarGlobalParameter p = (ScalarGlobalParameter) param;
        data += p.getQuantity().toString();
        data += ", ";
      }

      int step = sim.getSimulationIterator().getCurrentIteration();
      

      // Write the coefficient data to file
      sim.println("|--Writing coefficients...");
      for(DataSet report : getCoefficients()){
        data += report.getYValue(step - 1);
        data += ", ";
      }

      // Write the report data to file
      sim.println("|--Writing reports...");
      for(DataSet report : getReports()){
        data += report.getYValue(step - 1);
        data += ", ";
      }

      // Write meshing time
      data += meshTime + ", ";
      meshTime = 0;

      // Write meshing data
      FvRepresentation fvRep = ((FvRepresentation) sim.getRepresentationManager().getObject("Volume Mesh"));
      data += fvRep.getCellCount() + ", "; // Cell count
      data += fvRep.getInteriorFaceCount() + ", "; // Face count
      data += fvRep.getVertexCount() + ", "; // Vertex count

      // Write iteration
      data += step + ", ";

      // Write residuals and convergence factor
      sim.println("|--Writing residuals...");

      for(DataSet residual : getResiduals()){
        double val = residual.getYValue(step - 1);
        data += val + ", ";
      }
      
      // Write error
      data += error.replaceAll(",","/") + " , ";

      // Write comment
      data += comment.replaceAll(",","/") + "\n";

      bw.write(data);
      sim.println("Data Written: " + data);

      bw.close();
    } catch (IOException e) {
      sim.print(e.getLocalizedMessage());
    }
  }


  /**
   * Set the maximum number of steps the solution can run for
   * @param steps The max steps
   */
  private void setMaxSteps(int steps){
    Simulation sim = getActiveSimulation();

    StepStoppingCriterion stepStoppingCriterion = 
      (StepStoppingCriterion) sim.getSolverStoppingCriterionManager().getSolverStoppingCriterion("Maximum Steps");
      
    stepStoppingCriterion.setMaximumNumberSteps(steps);
  }

  /**
   * Set the units for the offsets of the tunnel / block of the simulation
   * @param back back offset units
   * @param front front offset units
   * @param side side offset units
   * @param top top offset units
   */
  private void setTunnelUnits(Units back, Units front, Units side, Units top){
    Simulation sim = getActiveSimulation();

    ScalarGlobalParameter tunnelBack = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Back Offset");
    tunnelBack.getQuantity().setUnits(back);

    ScalarGlobalParameter tunnelFront = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Front Offset");
    tunnelFront.getQuantity().setUnits(front);

    ScalarGlobalParameter tunnelSide = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Side Offset");
    tunnelSide.getQuantity().setUnits(side);

    ScalarGlobalParameter tunnelTop = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Top Offset");
    tunnelTop.getQuantity().setUnits(top);
  }
  /**
   * Set the offsets for the tunnel / block of the simulation
   * @param back back offset
   * @param front front offset
   * @param side side offset
   * @param top top offset
   */
  private void setTunnelOffsets(double back, double front, double side, double top){
    Simulation sim = getActiveSimulation();

    // Set back offset
    ScalarGlobalParameter tunnelBack = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Back Offset");
    tunnelBack.getQuantity().setValue(back);

    // Set front offset
    ScalarGlobalParameter tunnelFront = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Front Offset");
    tunnelFront.getQuantity().setValue(front);

    // Set side offset
    ScalarGlobalParameter tunnelSide = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Side Offset");
    tunnelSide.getQuantity().setValue(side);

    // Set top offset
    ScalarGlobalParameter tunnelTop = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Top Offset");
    tunnelTop.getQuantity().setValue(top);
  }

  /**
   * Set the units for all the meshing parameters
   * @param base
   * @param min
   * @param max
   * @param target
   * @param thick
   */
  private void setMeshingUnits(Units base, Units min, Units max, Units target, Units thick){
    Simulation sim = getActiveSimulation();

    // Set the base mesh size units
    ScalarGlobalParameter meshBase = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Base");
    meshBase.getQuantity().setUnits(base);

    // Set the min mesh size units
    ScalarGlobalParameter meshMin = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Min");
    meshMin.getQuantity().setUnits(min);

    // Set the max mesh size units
    ScalarGlobalParameter meshMax = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Max Tet");
    meshMax.getQuantity().setUnits(max);

    // Set the target mesh size units
    ScalarGlobalParameter meshTarget = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Target");
    meshTarget.getQuantity().setUnits(target);

    // Set the prism layer thickness units
    ScalarGlobalParameter prismLayerThickness = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Prism Layer Thickness");
    prismLayerThickness.getQuantity().setUnits(thick);
  }

  /**
   * Set the scalar values for all the meshing parameters
   * @param base
   * @param min
   * @param max
   * @param target
   * @param thick
   */
  private void setMeshingScalars(double ahmed, double base, double min, double max, double target, double thick){
    Simulation sim = getActiveSimulation();

    // Set Ahmed target size
    ScalarGlobalParameter ahmedTarget = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Ahmed Target Size");
    ahmedTarget.getQuantity().setValue(ahmed);

    // Set the base mesh size
    ScalarGlobalParameter meshBase = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Base");
    meshBase.getQuantity().setValue(base);

    // Set the min mesh size
    ScalarGlobalParameter meshMin = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Min");
    meshMin.getQuantity().setValue(min);

    // Set the max mesh size
    ScalarGlobalParameter meshMax = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Max Tet");
    meshMax.getQuantity().setValue(max);

    // Set the target mesh size
    ScalarGlobalParameter meshTarget = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Target");
    meshTarget.getQuantity().setValue(target);

    // Set the prism layer thickness
    ScalarGlobalParameter prismLayerThickness = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Prism Layer Thickness");
    prismLayerThickness.getQuantity().setValue(thick);
  }


  /**
   * Obtain a list of all parameters used
   * @return List of parameters
   */
  private List<NamedObject> getParameters(){
    Simulation sim = getActiveSimulation();
    Collection<NamedObject> params = sim.getGlobalParameterManager().getObjects();
    
    return new ArrayList<NamedObject>(params);
  }

  /**
   * Obtain a list of all coefficient data sets present in the simulation
   * @return List of coefficient data sets
   */
  private List<DataSet> getCoefficients(){
    Simulation sim = getActiveSimulation();
    StarPlot coefPlot = ((StarPlot) sim.getPlotManager().getPlot("Coefficients"));

    return new ArrayList<DataSet> (coefPlot.getDataSetManager().getObjects());
  }

    /**
   * Obtain a list of all report data sets present in the simulation
   * @return List of report data sets
   */
  private List<DataSet> getReports(){
    Simulation sim = getActiveSimulation();
    StarPlot reportPlot = ((StarPlot) sim.getPlotManager().getPlot("Reports"));

    return reportPlot.getDataSetCollection();
  }

  /**
   * Obtain a list of all residual data sets present in the simulation
   * @return List of residual data sets
   */
  private List<DataSet> getResiduals(){
    Simulation sim = getActiveSimulation();
    StarPlot residualPlot = ((StarPlot) sim.getPlotManager().getPlot("Residuals"));

    return residualPlot.getDataSetCollection();
  }

  
  /**
   * 
   * @return A string representation of the parameters
   */
  private String parametersToString(){
    String data = "";
    for(NamedObject param : getParameters()){
      ScalarGlobalParameter p = (ScalarGlobalParameter) param;
      data += p.getQuantity().toString();
      data += ", ";
    }
    data = data.substring(0, data.length() - 2);
    return data;
  }

  
  /**
   * A function to easily iterate through mesh settings
   */
  private void runMeshRange (double ahmed, double base, double target, double targetInc, double targetMax){
    Simulation sim = getActiveSimulation();
    for(; target <= targetMax; target += targetInc){
      // Set Ahmed target size
      ScalarGlobalParameter ahmedTarget = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Ahmed Target Size");
      ahmedTarget.getQuantity().setValue(ahmed);

      // Set the base mesh size
      ScalarGlobalParameter meshBase = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Base");
      meshBase.getQuantity().setValue(base);

      // Set the target mesh size
      ScalarGlobalParameter meshTarget = (ScalarGlobalParameter) sim.get(GlobalParameterManager.class).getObject("Mesh Target");
      meshTarget.getQuantity().setValue(target);

      runSim();
    }
  }

  /**
   * A function to easily iterate through tunnel settings
   */
  private void runTunnelRange(double back, double front, double side, double top, double lengthSpace, double frontalFactor, double maxLength, double maxFrontal){
    if(lengthSpace < 0 || frontalFactor < 0) return;
 
    for(double i = 0; i <= maxLength ; i+= lengthSpace){
      
      for(double j = 1; j <= maxFrontal ; j+= frontalFactor){
        setTunnelOffsets(i+back, i+front, j*side, j*top);
        runSim();

        if (j == 1 && frontalFactor > 1) j = 0;
      }
    }
  }
}