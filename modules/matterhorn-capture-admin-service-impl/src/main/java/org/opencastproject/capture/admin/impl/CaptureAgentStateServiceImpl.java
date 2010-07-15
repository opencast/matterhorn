/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.capture.admin.api.Recording;
//import org.opencastproject.capture.admin.api.RecordingStateUpdate;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.spi.PersistenceProvider;

/**
 * IMPL for the capture-admin service (MH-1336, MH-1394, MH-1457, MH-1475 and MH-1476).
 */
public class CaptureAgentStateServiceImpl implements CaptureAgentStateService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentStateServiceImpl.class);

  /** The name of the persistence unit for this class */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl";

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** The persistence properties */
  @SuppressWarnings("unchecked")
  protected Map persistenceProperties;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  private HashMap<String, Agent> agents;
  private HashMap<String, Recording> recordings;

  /**
   * @param persistenceProvider the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * @param persistenceProperties the persistenceProperties to set
   */
  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public CaptureAgentStateServiceImpl() {
    logger.info("CaptureAgentStateServiceImpl starting.");
    agents = new HashMap<String, Agent>();
    recordings = new HashMap<String, Recording>();
  }

  @SuppressWarnings("unchecked")
  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl", persistenceProperties);

    //Pull all the existing agents into the in-memory structure
    EntityManager em = emf.createEntityManager();
    List<AgentImpl> dbResults = null;
    Query q = em.createNamedQuery("AgentImpl.getAll");
    dbResults = (List<AgentImpl>) q.getResultList();
    em.close();

    if (dbResults != null) {
      for (Agent a : dbResults) {
        if (!agents.containsKey(a.getName())) {
          agents.put(a.getName(), a);
        }
      }
    }
  }

  public void deactivate() {
    if (emf != null) {
      emf.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentState(java.lang.String)
   */
  public Agent getAgentState(String agentName) {
    Agent req = agents.get(agentName);
    //If that agent doesn't exist, return an unknown agent, else return the known agent
    if (req == null) {
      logger.debug("Agent {} does not exist in the system.", agentName);
    } else {
      logger.debug("Agent {} found, returning state.", agentName);
    }
    return req;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentState(java.lang.String, java.lang.String)
   */
  public int setAgentState(String agentName, String state) {

    // Checks the state is not null nor empty
    if (state == null || state.equals("")) {
      logger.debug("Unable to set agent state, state is blank or null.");
      return BAD_PARAMETER;
    } else if (agentName == null || agentName.equals("")) {
      logger.debug("Unable to set agent state, agent name is blank or null.");
      return BAD_PARAMETER;
    }
 

    Agent req = agents.get(agentName);
    //if the agent is known set the state
    if (req != null) {
      logger.debug("Setting Agent {} to state {}.", agentName, state);
      req.setState(state);
      agents.put(agentName, req);
      updateAgentInDatabase(req);
    } else {     
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      logger.debug("Creating Agent {} with state {}.", agentName, state);
      Agent a = new AgentImpl(agentName, state, "", new Properties());
      agents.put(agentName, a);
      updateAgentInDatabase(a);
    }

    return OK;
  }
  
  public boolean setAgentUrl(String agentName, String agentUrl) {
    Agent req = agents.get(agentName);
    if(req != null) {
      req.setUrl(agentUrl);
      agents.put(agentName, req);
      updateAgentInDatabase(req);
    } else {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeAgent(java.lang.String)
   */
  public int removeAgent(String agentName) {
    if (agents.containsKey(agentName)) {
      logger.debug("Removing Agent {}.", agentName);
      agents.remove(agentName);
      deleteAgentFromDatabase(agentName);
      return OK;
    }
    return NO_SUCH_AGENT;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownAgents()
   */
  public Map<String, Agent> getKnownAgents() {
    return agents;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentCapabilities(java.lang.String)
   */
  public Properties getAgentCapabilities(String agentName) {

    Agent req = agents.get(agentName);
    //if the agent is known set the state
    if (req != null) {
      Properties temp = agents.get(agentName).getCapabilities();
      return temp;
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setAgentCapabilities
   */
  public int setAgentCapabilities(String agentName, Properties capabilities) {
    Agent req = agents.get(agentName);
    if (req != null) {
      logger.debug("Setting Agent {}'s capabilities", agentName);
      req.setCapabilities(capabilities);
      updateAgentInDatabase(req);
    } else {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      if (agentName == null || agentName.equals("")) {
        logger.debug("Unable to set agent state, agent name is blank or null.");
        return BAD_PARAMETER;
      }

      logger.debug("Creating Agent {} with state {}.", agentName, AgentState.UNKNOWN);
      Agent a = new AgentImpl(agentName, AgentState.UNKNOWN, "", capabilities);
      agents.put(agentName, a);
      updateAgentInDatabase(a);
    }

    return OK;
  }

  /**
   * Updates or adds an agent to the database.
   * @param a The Agent you wish to modify or add in the database.
   */
  private synchronized void updateAgentInDatabase(Agent a) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      tx = em.getTransaction();
      tx.begin();
      Agent existing = em.find(AgentImpl.class, a.getName());
      if (existing != null) {
        existing.setCapabilities(a.getCapabilities());
        existing.setLastHeardFrom(a.getLastHeardFrom());
        existing.setState(a.getState());
        em.merge(existing);
      } else {
        em.persist(a);
      }
      tx.commit();
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in updateAgent.");
    } finally {
      em.close();
    }
  }

  /**
   * Removes an agent from the database.
   * @param agentName The name of the agent you wish to remove.
   */
  private synchronized void deleteAgentFromDatabase(String agentName) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      tx = em.getTransaction();
      tx.begin();
      Agent existing = em.find(AgentImpl.class, agentName);
      if (existing != null) {
        em.remove(existing);
      }
      tx.commit();
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in deleteAgent.");
    } finally {
      em.close();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getRecordingState(java.lang.String)
   */
  public Recording getRecordingState(String id) {
    Recording req = recordings.get(id);
    //If that recording doesn't exist, return null
    if (req == null)
      logger.debug("Recording {} does not exist in the system.", id);
    else
      logger.debug("Recording {} found, returning state.", id);
    
    return req;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#setRecordingState(java.lang.String, java.lang.String)
   */
  public boolean setRecordingState(String id, String state) {
    Recording req = recordings.get(id);
    if (req != null) {
      logger.debug("Setting Recording {} to state {}.", id, state);
      req.setState(state);
      return true;
    } else {
      if (id == null || id.equals("")) {
        logger.debug("Unable to set recording state, recording name is blank or null.");
        return false;
      } else if (state == null || state.equals("")) {
        logger.debug("Unable to set recording state, recording state is blank or null.");
        return false;
      }
      logger.debug("Creating Recording {} with state {}.", id, state);
      Recording r = new RecordingImpl(id, state);
      recordings.put(id, r);
      return true;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeRecording(java.lang.String)
   */
  public boolean removeRecording(String id) {
    logger.debug("Removing Recording {}.", id);
    if (recordings.remove(id) != null) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownRecordings()
   */
  public Map<String,Recording> getKnownRecordings() {
    return recordings;
  }
  
  public List<String> getKnownRecordingsIds() {
    LinkedList<String> ids = new LinkedList<String>();
    for (Entry<String, Recording> e : recordings.entrySet()) {
      ids.add(e.getValue().getID());
    }
    return ids;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary props) throws ConfigurationException {

  }
}
