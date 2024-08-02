package org.wso2.carbon.module.ai;

import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.module.ai.constants.AIConstants;
import org.wso2.carbon.module.ai.model.prompt.AIConverserAgentModel;
import org.wso2.carbon.module.ai.util.AIUtils;

/**
 * Mediator class that handles execution of a given prompt with AI.
 */
public class AIConverser extends AbstractMediator {

    @Override
    public boolean mediate(MessageContext synCtx) {
        org.apache.axis2.context.MessageContext msgContext = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        try {
            AIConverserAgentModel agent = AIUtils.getAIConverserAgent(synCtx);
            agent.processRequest();
            JsonUtil.getNewJsonPayload(msgContext, agent.getResponse(), true, true);
            msgContext.setProperty(AIConstants.MESSAGE_TYPE_STRING, AIConstants.JSON_CONTENT_TYPE);
            msgContext.setProperty(AIConstants.CONTENT_TYPE_STRING, AIConstants.JSON_CONTENT_TYPE);
        } catch (SynapseException e) {
            handleException(e.getMessage(), synCtx);
        } catch (AxisFault e) {
            handleException("JsonUtil error.", e, synCtx);
        }
        return true;
    }
}
