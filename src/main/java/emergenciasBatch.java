import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.force.api.ApiSession;
import com.force.api.DescribeSObject;
import com.force.api.DescribeSObject.Field;
import com.force.api.DescribeSObject.PicklistEntry;
import com.force.api.ForceApi;
import com.sforce.soap.metadata.FieldType;

public class emergenciasBatch {

    private static final String clientId = "3MVG9jfQT7vUue.G57q9YxkeioLB1AE02bGctQ09wjCAcxfXK1OFwgt1YXFGm8HdTaXvIRCmF1pG259r4SsTz";
    private static final String clientSecret = "4907478524393614560";
    private static final String username = "aalonsotrinidad@enel.emerdev";
    private static final String password = "Del*Alvaro1606EI63ahdpjJOLR1T1vPQZN9lb";
    private static String accessToken = null;
    private static String instanceUrl = null;
    private static final String redirectUri = "https://localhost:8443/_callback";
    private static final String environment = "https://test.salesforce.com";   
    private static String tokenUrl = null;
    
    public static void main(String[] args){
        System.out.println("OneOffProcess executed.");
        try{
        	List<PickList> listaRecuperadaSF = recuperarPickList();
        	if (listaRecuperadaSF != null && !listaRecuperadaSF.isEmpty()){
        		generarDatosEnPostgre(listaRecuperadaSF);
        	}
        }catch(Exception ex){
            ex.printStackTrace();
            System.out.println("Error: " + ex.getCause());
        }
        
    }
   
    /*Rest API Salesforce*/
	private static List<PickList> recuperarPickList(){
		List<PickList> listaDatos = null;
		Map<String, String> mapAux = null;
		ForceApi force = getForceApi();
		if (force != null){
			System.out.println("Tengo sesión");
			//Recuperar objetos de fichero de configuración
			Properties props = new Properties();
			try(InputStream resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("picklist.properties")) {
			    props.load(resourceStream);
			}catch(Exception ex){
		        System.out.println("Error: " + ex.getCause());
			}
			if (props != null && props.containsKey("pickList.implicados")){
				String objetos = props.getProperty("pickList.implicados");
				if (objetos != null){
					String[] objCreacion = objetos.split(",");
					listaDatos = new ArrayList<PickList>();
					List<PickList> listAux = null;
					for (Integer i = 0; i<objCreacion.length; i++){
						listAux = recuperarDatosDescribe(force, objCreacion[i]);
						if (listAux != null && !listAux.isEmpty()){
							listaDatos.addAll(listAux);
						}
					}
					
				}
			}

		}
		return listaDatos;
	}
	
	/*Rest API Salesforce*/
	
	private static ForceApi getForceApi(){
		loginSalesforce();
		if (accessToken != null){
	        ApiSession s = new ApiSession();
	        s.setAccessToken(accessToken);
	        s.setApiEndpoint(instanceUrl);
	        return new ForceApi(s);
		}
		return null;
    }
	
    
	private static void loginSalesforce(){ 
        // Step 0:  Connect to SalesForce.
        System.out.println("Getting a token");
        tokenUrl = environment + "/services/oauth2/token";
        HttpClient httpclient = new HttpClient();
        PostMethod post = new PostMethod(tokenUrl);     
        post.addParameter("grant_type", "password");
        post.addParameter("client_id", clientId);
        post.addParameter("client_secret", clientSecret);
        post.addParameter("redirect_uri", redirectUri);
        post.addParameter("username", username);
        post.addParameter("password", password);
        System.out.println("Intento de llamada POST");
        try {
        	
        	System.out.println("Request: " + post.getRequestEntity());
            httpclient.executeMethod(post);
            try {
                JSONObject authResponse = new JSONObject(new JSONTokener(new InputStreamReader(post.getResponseBodyAsStream())));
                System.out.println("Auth response: " + authResponse.toString(2));

                accessToken = authResponse.getString("access_token");
                instanceUrl = authResponse.getString("instance_url");

                System.out.println("Token de acceso: " + accessToken);
            } catch (JSONException e) {
                e.printStackTrace();                
            }
        } catch (HttpException e1) {
            e1.printStackTrace();
            System.out.println("Error: " + e1.getCause());
        } catch (IOException e1) {
            e1.printStackTrace();
            System.out.println("Error: " + e1.getCause());
        } finally {
            post.releaseConnection();
        }       
        System.out.println("Tenemos token de acceso: " + accessToken + "\n" + "para la instancia " + instanceUrl + "\n\n");
    }
 
    private static List<PickList> recuperarDatosDescribe(ForceApi force, String objetoSalesforce){
    	List<PickList> listaPickList = null;
    	PickList objPickList = null;
    	DescribeSObject objDescribe = force.describeSObject(objetoSalesforce);
		System.out.println("Recupera objeto describe");
		// Get sObject metadata
		if (objDescribe != null){
			System.out.println("sObject name: " +objDescribe.getName());
	        List<Field> fields = objDescribe.getFields();
	        List<PicklistEntry> picklistValues = null;
		    System.out.println("Has " + fields.size() + " fields");
		        // Iterate through each field and gets its properties
	    	Field field = null;
	        for (int i = 0; i < fields.size(); i++) {
	          field = fields.get(i);
	          // If this is a picklist field, show the picklist values
	          if (FieldType.Picklist.toString().equalsIgnoreCase(field.getType())) {
	              picklistValues = field.getPicklistValues();
	              System.out.println("Picklist recuperado: " + picklistValues);
	              if (picklistValues != null) {
	                for (int j = 0; j < picklistValues.size(); j++) {
		            	if (listaPickList ==null){
		            		listaPickList = new ArrayList<PickList> ();
		            	}
		            	objPickList = new PickList();
		            	objPickList.setObjeto(objetoSalesforce);
		            	objPickList.setCampo(field.getName());
		            	objPickList.setCodigo(picklistValues.get(j).getValue());
		            	objPickList.setValor(picklistValues.get(j).getLabel());
		            	listaPickList.add(objPickList);
	                	System.out.println("\tItem: Value: " + picklistValues.get(j).getValue() +
	                			" .Descipción: " + picklistValues.get(j).getLabel());
	                  }
	                }
              }
	        }
		}
		return listaPickList;
    }
    /*Fin Rest API Salesforce*/
    
    /*Generar objetos en postgree*/
    private static void generarDatosEnPostgre(List<PickList> listaRecuperadaSF){
	    
	    Connection connection = null;
	    Statement stmt = null;
	    PreparedStatement pstmt= null;
	    try{
		    //Driver de postgree
	    	Class.forName("org.postgresql.Driver");
	        System.out.println("Recuperando conexión");
	        //Conexion con los datos de postgree
	        //connection = DriverManager.getConnection("jdbc:postgresql://ec2-23-21-102-155.compute-1.amazonaws.com:5432/d5418fkq019rpl","dhtsyocojobsrd", "cjB0mRnKEn8XeK2RBwkjAipxoJ");
	        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres-Enel","postgres", "deloitte12");
	        connection.setAutoCommit(false);
	        System.out.println("Connectado correctamente");
	        stmt = connection.createStatement();
	        Boolean existeTabla = false;
	        //Comprobar si ya existe la tabla
	        DatabaseMetaData md = connection.getMetaData();
	        ResultSet rs = md.getTables(null, null, "%", null);
	        while (rs.next()) {
	          System.out.println("ResultSet name: " + rs.getString(3));
	          if ("picklists".equalsIgnoreCase(rs.getString(3))){
	        	  existeTabla = true;
	        	  break;
	          }  
	        }
	        
	        if (!existeTabla){
	        	System.out.println("Inicio creación tabla.");
		        stmt.executeUpdate(crearTabla());
		        System.out.println("Tabla creada.");
	        }else{
	        	System.out.println("Inicio borrado datos.");
	        	stmt.executeUpdate("Delete from salesforce.picklists");
	        	System.out.println("Fin borrado datos.");
	        }
	        System.out.println("Inicio actualización de datos.");
	        //String sql = recuperaInsertUpdateQuery(listaRecuperadaSF, pickListBBDD);
            pstmt = connection.prepareStatement("INSERT INTO salesforce.picklists (id, Objeto, Campo, codigo, valor) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        	PickList obkPickList = null;
			for (Integer i=0; i<listaRecuperadaSF.size();i++){
				obkPickList = listaRecuperadaSF.get(i);
				pstmt.setInt(1, i);
	            pstmt.setString(2, obkPickList.getObjeto());
	            pstmt.setString(3, obkPickList.getCampo());
	            pstmt.setString(4, obkPickList.getCodigo());
	            pstmt.setString(5, obkPickList.getValor());
	            pstmt.addBatch();
			}
			pstmt.executeBatch();

	        connection.commit();
	     }catch(SQLException se){
	        se.printStackTrace();
	        System.out.println("Error: " + se.getCause());
	     }catch(Exception e){
	        e.printStackTrace();
	        System.out.println("Error: " + e.getCause());
	     }finally{
	        try{
	           if(stmt!=null){
	        	   connection.close();
	           }
	        }catch(SQLException se){
	        }
	        try{
	           if(connection!=null){	        	   
	        	   connection.close();
	           }
	        }catch(SQLException se){
	           se.printStackTrace();
	        }
	     }    
    }

	private static String crearTabla(){
        return "CREATE TABLE salesforce.picklists " +
                     "(id integer NOT NULL, " +	                     
                     " Objeto VARCHAR(255), " + 
                     " Campo VARCHAR(255), " + 
                     " codigo VARCHAR(255), " +
                     " valor VARCHAR(255), " + 
                     " PRIMARY KEY ( id ))";
	}
    /*Fin Generar objetos en postgree*/
}
