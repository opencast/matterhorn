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

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
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
  protected Map persistenceProperties;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  private HashMap<String, Agent> agents;
  private HashMap<String, Recording> recordings;
  private List<String> stickyAgents;
  private Semaphore sem;

  /**
   * @param persistenceProvider the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * @param persistenceProperties the persistenceProperties to set
   */
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public CaptureAgentStateServiceImpl() {
    logger.info("CaptureAgentStateServiceImpl starting.");
    agents = new HashMap<String, Agent>();
    recordings = new HashMap<String, Recording>();
    stickyAgents = new LinkedList<String>();
  }

  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.capture.admin.impl.CaptureAgentStateServiceImpl", persistenceProperties);
    sem = new Semaphore(1);
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
      req = findAgent(agentName);
      if (req == null) {
        logger.debug("Agent {} does not exist in the system.", agentName);
      }
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
    }

    Agent req = agents.get(agentName);
    //if the agent is known set the state
    if (req != null) {
      logger.debug("Setting Agent {} to state {}.", agentName, state);
      req.setState(state);
      updateAgent(req);
    } else {     
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      if (agentName == null || agentName.equals("")) {
        logger.debug("Unable to set agent state, agent name is blank or null.");
        return BAD_PARAMETER;
      }

      logger.debug("Creating Agent {} with state {}.", agentName, state);
      Agent a = new Agent(agentName, state, new Properties());
      agents.put(agentName, a);
      updateAgent(a);
    }

    return OK;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeAgent(java.lang.String)
   */
  public int removeAgent(String agentName) {
    if (agents.containsKey(agentName)) {
      logger.debug("Removing Agent {}.", agentName);
      agents.remove(agentName);
      deleteAgent(agentName);
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
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getAgentCapabilities()
   */
  public Properties getAgentCapabilities(String agentName) {

    Agent req = agents.get(agentName);
    //if the agent is known set the state
    if (req != null) {
      Properties temp = agents.get(agentName).getCapabilities();
      return temp;
    }   
    return null;  
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
      updateAgent(req);
    } else {
      // If the agent doesn't exists, but the name is not null nor empty, create a new one.
      if (agentName == null || agentName.equals("")) {
        logger.debug("Unable to set agent state, agent name is blank or null.");
        return BAD_PARAMETER;
      }

      logger.debug("Creating Agent {} with state {}.", agentName, AgentState.UNKNOWN);
      Agent a = new Agent(agentName, AgentState.UNKNOWN, capabilities);
      agents.put(agentName, a);
      updateAgent(a);
    }

    return OK;
  }

  private Agent findAgent(String agentName) {
    EntityManager em = emf.createEntityManager();
    Agent a = null;
    try {
      a = em.find(Agent.class, agentName);
    } catch (NoResultException e) {
      logger.debug("Unable to find agent {} in database.", agentName);
    } catch (NonUniqueResultException e) {
      logger.warn("Found multiple agents named {} in the DB.  This should be fixed!", agentName);
    } finally {
      em.close();
    }
    return a;
  }

  /**
   * Updates or adds an agent to the database.
   * @param a The Agent you wish to modify or add in the database.
   */
  private void updateAgent(Agent a) {
    if (stickyAgents.contains(a.getName())) {
      EntityManager em = emf.createEntityManager();
      EntityTransaction tx = null;
      try {
        sem.acquire();
        tx = em.getTransaction();
        tx.begin();
        Agent existing = em.find(Agent.class, a.getName());
        if (existing != null) {
          existing.setCapabilities(a.getCapabilities());
          existing.setLastHeardFrom(a.getLastHeardFrom());
          existing.setState(a.getState());
          em.merge(existing);
        } else {
          em.persist(a);
        }
        tx.commit();
        sem.release();
      } catch (RollbackException e) {
        logger.warn("Unable to commit to DB in updateAgent.");
      } catch (InterruptedException e) {
        logger.warn("Semaphore broken in updateAgent, DB may be inconsistent.");
      } finally {
        em.close();
      }
    }
  }

  /**
   * Removes an agent from the database.
   * @param agentName The name of the agent you wish to remove.
   */
  private void deleteAgent(String agentName) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      sem.acquire();
      tx = em.getTransaction();
      tx.begin();
      Agent existing = em.find(Agent.class, agentName);
      if (existing != null) {
        em.remove(existing);
      }
      tx.commit();
      sem.release();
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in deleteAgent.");
    } catch (InterruptedException e) {
      logger.warn("Semaphore broken in deleteAgent, DB may be inconsistent.");
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
  public void setRecordingState(String id, String state) {
    Recording req = recordings.get(id);
    if (req != null) {
      logger.debug("Setting Recording {} to state {}.", id, state);
      req.setState(state);
    } else {
      if (id == null || id.equals("")) {
        logger.debug("Unable to set recording state, recording name is blank or null.");
        return;
      } else if (state == null || state.equals("")) {
        logger.debug("Unable to set recording state, recording state is blank or null.");
        return;
      }
      logger.debug("Creating Recording {} with state {}.", id, state);
      Recording r = new Recording(id, state);
      recordings.put(id, r);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#removeRecording(java.lang.String)
   */
  public void removeRecording(String id) {
    logger.debug("Removing Recording {}.", id);
    recordings.remove(id);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.capture.admin.api.CaptureAgentStateService#getKnownRecordings()
   */
  public Map<String,Recording> getKnownRecordings() {
    return recordings;
  }

  public void updated(Dictionary props) throws ConfigurationException {
    String agentList = (String) props.get(CaptureAgentStateService.STICKY_AGENTS);
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = null;
    try {
      if (agentList != null) {
        //Get the list of agents to keep
       String[] splitList = agentList.split(",");
       LinkedList<String> t = new LinkedList<String>();
       for (String agentName : splitList) {
         logger.debug("Adding " + agentName + " to sticky list");
         t.add(agentName);
       }
       stickyAgents = t;

       sem.acquire();

       //Remove those who are not in the list
       List<Agent> dbAgents = em.createNamedQuery("Agent.getAll").getResultList();
       tx = em.getTransaction();
       tx.begin();
       for (Agent a : dbAgents) {
         if (!stickyAgents.contains(a.getName())) {
           logger.debug("Removing " + a.getName());
           em.remove(a);
         } else {
           logger.debug("Skipping " + a.getName());
         }
       }
       tx.commit();

       //Add and update those who are in the list
       tx = em.getTransaction();
       tx.begin();
       for (String name : stickyAgents) {
         Agent a = agents.get(name);
         if (a != null) {
           logger.debug("Adding " + a.getName());
           em.persist(a);
         }
       }
       tx.commit();

       sem.release();
      } else {
        sem.acquire();
        //The agent list was blank, so remove all of the agents which are already in the db
        List<Agent> dbAgents = em.createNamedQuery("Agent.getAll").getResultList();
        tx = em.getTransaction();
        tx.begin();
        for (Agent a : dbAgents) {
          logger.debug("Removing all: " + a.getName());
          em.remove(a);
        }
        tx.commit();
        sem.release();
      }
    } catch (RollbackException e) {
      logger.warn("Unable to commit to DB in updated.");
    } catch (InterruptedException e) {
      logger.warn("Semaphore broken in updated, DB may be inconsistent.");
    }
  }
}
