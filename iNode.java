import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for a remote instance of a node in a DHT ring
 * @author Kat Winter
 */
public interface iNode extends Remote {
	
	/**
	 * @return hashed IP address of this node
	 * @throws RemoteException
	 */
	String getID() throws RemoteException;
	
	/**
	 * Removes a key/value pair from the DHT
	 * @param k the unhashed key for removal
	 * @throws RemoteException
	 */
	void remove(String k) throws RemoteException;
	
	/**
	 * Transfer appropriate data over to a new Node
	 * Precondition: The new Node is located directly previous
	 * to this Node and has just joined the Ring
	 * @param newNode Node just joining the ring
	 * @throws RemoteException
	 */
	void transferData(iNode newNode) throws RemoteException;
	
	/**
	 * Insert a key/data pair directly to this Node
	 * Does not perform any check in this method to see if the
	 * values should be stored at this Node
	 * @param key Hashed key to be stored
	 * @param data Data to be stored corresponding to the key parameter
	 * @throws RemoteException
	 */
	void insert(String key, Object data) throws RemoteException;
	
	/**
	 * Add the specified Node into the Ring at appropriate location
	 * based on the hashed key of the Node's IP
	 * @param node The node to be added
	 * @throws RemoteException
	 */
	void addNodeToRing(iNode node) throws RemoteException;
	
	/**
	 * Update on what Node directly follows this Node
	 * @param node Node now following this Node
	 * @throws RemoteException
	 */
	void setNext(iNode node) throws RemoteException;
	
	/**
	 * Update on what Node directly precedes this Node
	 * @param node Node now preceding this Node
	 * @throws RemoteException
	 */
	void setPrev(iNode node) throws RemoteException;
	
	/**
	 * Return the requested data item based on hashing the key k.
	 * @param k Unhashed key corresponding to the value/data that requester needs
	 * @return The value/data corresponding to the Key k
	 * @throws RemoteException
	 */
	Object get(String k) throws RemoteException;
	
	/**
	 * Store value/data matching a hash of the given Key k
	 * @param k Unhashed key corresponding to the given data
	 * @param data The data to be stored at this node according to key K
	 * @throws RemoteException
	 */
	void put(String k, Object data) throws RemoteException;
}
