package drugmodel;

import java.util.LinkedHashMap;

public class EdgeDataMap<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 1L;

	public void addData(K key, V data) {
		/* We can make it faster since we are using a LinkedHashMap here. */
		if (!containsKey(key)) {
			put(key, data);
		}
	}

	public void removeInformationData(K key, V data) {
		if (isEmpty()) {
			if (Settings.errorLog) {
				System.out.println("Attempt to remove non-existent information data. key: " + key + " object: " + data.toString());
			}
		}
		if (containsKey(key)) {
			remove(data);
		}			
	}

	public V returnLastEntry() {
		//Integer lastKey = informationData.keySet()	
		if (size() == 0) {
			if (Settings.errorLog) {
				System.out.println("Attempt to retrieve last information from an empty EdgeDataMap. Returning nul.");
			}
			return null;
		}
		return get(keySet().toArray()[size()-1]);
	}
	
	public V returnFirstEntry() {
		if (size() == 0) {
			if (Settings.errorLog) {
				System.out.println("Attempt to retrieve last information from an empty EdgeDataMap. Returning null.");
			}
			return null;
		}
		return get(keySet().iterator().next());
	}	
}