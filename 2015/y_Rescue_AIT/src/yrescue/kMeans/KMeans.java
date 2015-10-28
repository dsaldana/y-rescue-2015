package yrescue.kMeans;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.*;

public class KMeans {
	private int k;
    private List<Building> buildings;
    private List<EntityID> partition;
    private Map<EntityID, EntityID> classification;
    
    // Construtor: Definir numero de particoes
	public KMeans(int numberPartitions) {
		k = numberPartitions;
		buildings = new ArrayList<Building>();
		partition = new ArrayList<EntityID>();
		classification = new HashMap<EntityID, EntityID>(); 
	}
	
	public List<EntityID> getPartitions(){
		return this.partition;
	}
	
	// Separar o mapa em K celulas
    public Map<EntityID, EntityID> calculatePartitions(StandardWorldModel model){
    	 
    	 // Listas auxliares
    	 Collection<StandardEntity> allBuildings = model.getEntitiesOfType(StandardEntityURN.BUILDING);
    	 List<EntityID> partitionAnterior = new ArrayList<EntityID>();
    	 
    	 // Dividir o vetor em intervalos iguais, quando possivel, para a selecao aleatoria dos centroides 
    	 int interval = allBuildings.size()/k;
    	 
    	 //////////////////////////////////////////////////
    	 // Gerar os k primeiros centroides das k celulas
    	 int count = 0;
    	 // Pegar o valor no centro ou proximo ao centro de cada intervalo do vetor
    	 int mInterval = interval/2;
    	 // >> Exemplo: 
    	 //  Vetor com tamanho = 6 Intervalo = 2 Meio do Intervalo = 1
    	 //  Vetor | - | X | - | X | - | X |  
    	 //  index   0   1   2   3   4   5
    	 //  count   0   1   0   1   0   1
         for(StandardEntity next : allBuildings){
             if (next instanceof Building) {
                 Building b = (Building)next;
                 buildings.add(b);
                 if(count == mInterval){
                	 partition.add(b.getID());
                 }
             } else {
            	 Logger.debug("Not a Building");
             }
             count++;
             if(count == interval) count = 0;
         }
                  
         // Auxiliares
    	 double distance = Integer.MAX_VALUE, distanceTemp = Integer.MAX_VALUE;
    	 EntityID idTemp = partition.get(0);
    	 float[] mediaX = new float[k];
         float[] mediaY = new float[k];
         int[] countBCelula = new int[k];
         int flagEstabilizar = 0;
         
         ////////////////////////////////
         //// Particionar ate estabilizar
         while(flagEstabilizar == 0){   
        	 classification.clear();
        	 // Analisar todos os predios
	         for(Building nextB : buildings){
	        	 // Iniciar valores (problema de minimizacao)
	        	 distance = Integer.MAX_VALUE;
	        	 distanceTemp = Integer.MAX_VALUE;
	        	 // Verificar qual particao e a mais proxima do predio em teste
	        	 for(EntityID nextP : partition){
	        		 StandardEntity temp = model.getEntity(nextP);
	        		 Building b = (Building)temp;
        			 distanceTemp = Math.sqrt( Math.pow(nextB.getX()/100-b.getX()/100,2)+Math.pow(nextB.getY()/100-b.getY()/100,2) );
        			 if(distanceTemp < distance){
        				 distance = distanceTemp;
        				 idTemp = nextP;
        			 }
	        	 }	
	        	 
	        	 // Somar os valores da posicao de cada predio para cada particao
	        	 mediaX[partition.indexOf(idTemp)] += nextB.getX();
	        	 mediaY[partition.indexOf(idTemp)] += nextB.getY();
	        	 // Somar o numero de predio por particao
	        	 countBCelula[partition.indexOf(idTemp)]++;
	        	 // Classificar o predio
	        	 classification.put(nextB.getID(), idTemp);
	         }
	         
	         ///////////////////////////
	         // Definir novas celulas
	         // Primeiro, identificar a media de cada particao para a posicao em x e y
	         for(int i = 0; i < k; i++){
		         mediaX[i] /= countBCelula[i];
		    	 mediaY[i] /= countBCelula[i];
		    	 countBCelula[i] = 0;
	         }
	         // Depois, verificar qual construcao esta mais proximo da media 
	         // e defini-lo como o centro da nova celula
	         distance = Integer.MAX_VALUE;
	    	 distanceTemp = Integer.MAX_VALUE;
	    	 for(int i = 0; i < k; i++){
		         for(Building nextB : buildings){
		        	 distanceTemp = Math.sqrt( Math.pow(mediaX[i]/100-nextB.getX()/100,2)+Math.pow(mediaY[i]/100-nextB.getY()/100,2) );
	    			 if(distanceTemp < distance){
	    				 distance = distanceTemp;
	    				 idTemp = nextB.getID();
	    			 }
		         }
		         distance = Integer.MAX_VALUE;
		    	 distanceTemp = Integer.MAX_VALUE;
		         partition.set(i, idTemp);
	         }
	    	 ////////////////////////////
	    	 // Verificar se estabilizou
	         if(partitionAnterior.size() != 0){
	        	 for(int z = 0; z < k; z++){
		        	 if(partition.get(z).getValue() != partitionAnterior.get(z).getValue()){
		        		 flagEstabilizar = 0;
		        		 break;
		        	 }else flagEstabilizar = 1;
		         } 
	         }
	         // Atualizar vetor partitionAnterior
	         partitionAnterior.clear();
	         for(EntityID nextP : partition){
	        	 partitionAnterior.add(nextP);
	         }
   		 }      
         Logger.info(":: Numero de Predios =  " + allBuildings.size());
         Logger.info(":: Particoes =  " + partition);
         Logger.info(":: Classificacao =  " + classification);
         return classification;
    } 
}
