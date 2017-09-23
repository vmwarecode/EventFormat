/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.events;

import com.vmware.common.annotations.Action;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * <pre>
 * EventFormat
 *
 * This sample retrieves and formats the lastEvent from Hostd or Vpxd
 *
 * <b>Parameters:</b>
 * url          [required] : url of the web service.
 * username     [required] : username for the authentication.
 * password     [required] : password for the authentication
 *
 * <b>Command Line:</b>
 * run.bat com.vmware.events.EventFormat --url [webserviceurl]
 * --username [username] --password [password]
 * </pre>
 */
@Sample(name = "event-format", description = "This sample retrieves and formats the lastEvent from Hostd or Vpxd")
public class EventFormat extends ConnectedVimServiceBase {

    ManagedObjectReference eventManagerRef;
    ManagedObjectReference propCollectorRef;

    /**
     * Creates the event filter Spec.
     *
     * @return the PropertyFilterSpec
     */
    PropertyFilterSpec createEventFilterSpec(String name) {
        PropertySpec propSpec = new PropertySpec();
        propSpec.setAll(new Boolean(false));
        propSpec.getPathSet().add(name);
        propSpec.setType(eventManagerRef.getType());

        ObjectSpec objSpec = new ObjectSpec();
        objSpec.setObj(eventManagerRef);
        objSpec.setSkip(new Boolean(false));

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(propSpec);
        spec.getObjectSet().add(objSpec);
        return spec;
    }

    void formatLatestEvent() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        ArrayList<PropertyFilterSpec> listpfs =
                new ArrayList<PropertyFilterSpec>();
        listpfs.add(createEventFilterSpec("description.eventInfo"));
        List<ObjectContent> listobjcont =
                retrievePropertiesAllObjects(listpfs);
        if (listobjcont != null) {
            ArrayOfEventDescriptionEventDetail arrayEventDetails =
                    (ArrayOfEventDescriptionEventDetail) listobjcont.get(0)
                            .getPropSet().get(0).getVal();
            List<EventDescriptionEventDetail> eventDetails =
                    arrayEventDetails.getEventDescriptionEventDetail();
            Hashtable<String, EventDescriptionEventDetail> eventDetail =
                    new Hashtable<String, EventDescriptionEventDetail>();
            for (EventDescriptionEventDetail ed : eventDetails) {
                eventDetail.put(ed.getKey(), ed);
            }
            ArrayList<PropertyFilterSpec> listpfsevent =
                    new ArrayList<PropertyFilterSpec>();
            listpfsevent.add(createEventFilterSpec("latestEvent"));
            List<ObjectContent> listobjcontevent =
                    retrievePropertiesAllObjects(listpfsevent);
            if (listobjcontevent != null) {
                Event anEvent =
                        (Event) listobjcontevent.get(0).getPropSet().get(0)
                                .getVal();

                // Get the 'latestEvent' property of the EventManager
                System.out.println("The latestEvent was : "
                        + anEvent.getClass().getName());
                formatEvent(0, eventDetail, anEvent);
                formatEvent(1, eventDetail, anEvent);
                formatEvent(2, eventDetail, anEvent);
                formatEvent(3, eventDetail, anEvent);
                formatEvent(4, eventDetail, anEvent);
            }
        } else {
            System.out.println("No Events retrieved!");
        }
    }

    void formatEvent(int fType,
                     Hashtable<String, EventDescriptionEventDetail> eventDetail,
                     Event theEvent) {
        String typeName = theEvent.getClass().getName();
        // Remove package information...
        int lastDot = typeName.lastIndexOf('.');
        if (lastDot != -1) {
            typeName = typeName.substring(lastDot + 1);
        }
        EventDescriptionEventDetail detail = eventDetail.get(typeName);
        // Determine format string
        String format = detail.getFullFormat();
        switch (fType) {
            case 2:
                format = detail.getFormatOnComputeResource();
                break;
            case 3:
                format = detail.getFormatOnDatacenter();
                break;
            case 1:
                format = detail.getFormatOnHost();
                break;
            case 0:
                format = detail.getFormatOnVm();
                break;
            case 4:
                format = detail.getFullFormat();
                break;
        }
        String ret = "";
        if ("VmPoweredOnEvent".equals(typeName)) {
            ret = replaceText(format, (VmPoweredOnEvent) theEvent);
            if (ret != null) {
                System.out.println(ret);
            }
        } else if ("VmRenamedEvent".equals(typeName)) {
            ret = replaceText(format, (VmRenamedEvent) theEvent);
            if (ret != null) {
                System.out.println(ret);
            }
        } else if ("UserLoginSessionEvent".equals(typeName)) {
            ret = replaceText(format, (UserLoginSessionEvent) theEvent);
            if (ret != null) {
                System.out.println(ret);
            }
        } else {
            // Try generic, if all values are replaced by base type
            // return that, else return fullFormattedMessage;
            ret = replaceText(format, theEvent);
            if (ret.length() == 0 || ret.indexOf("{") != -1) {
                ret = theEvent.getFullFormattedMessage();
            }
            if (ret != null) {
                System.out.println(ret);
            }
        }
    }

    String replaceText(String format,
                       UserLoginSessionEvent theEvent) {
        // Do base first
        format = replaceText(format, (Event) theEvent);
        // Then specific values
        format = format.replaceAll("\\{ipAddress\\}", theEvent.getIpAddress());
        return format;
    }

    String replaceText(String format, VmPoweredOnEvent theEvent) {
        // Same as base type
        return replaceText(format, (Event) theEvent);
    }

    String replaceText(String format, VmRenamedEvent theEvent) {
        // Do base first
        format = replaceText(format, (Event) theEvent);
        // Then specific values
        format = format.replaceAll("\\{oldName\\}", theEvent.getOldName());
        format = format.replaceAll("\\{newName\\}", theEvent.getNewName());
        return format;
    }

    String replaceText(String format, Event theEvent) {
        format = format.replaceAll("\\{userName\\}", theEvent.getUserName());
        if (theEvent.getComputeResource() != null) {
            format =
                    format.replaceAll("\\{computeResource.name\\}", theEvent
                            .getComputeResource().getName());
        }
        if (theEvent.getDatacenter() != null) {
            format =
                    format.replaceAll("\\{datacenter.name\\}", theEvent
                            .getDatacenter().getName());
        }
        if (theEvent.getHost() != null) {
            format =
                    format.replaceAll("\\{host.name\\}", theEvent.getHost()
                            .getName());
        }
        if (theEvent.getVm() != null) {
            format =
                    format.replaceAll("\\{vm.name\\}", theEvent.getVm().getName());
        }
        return format;
    }

    /**
     * Uses the new RetrievePropertiesEx method to emulate the now deprecated
     * RetrieveProperties method.
     *
     * @param listpfs
     * @return list of object content
     * @throws Exception
     */
    List<ObjectContent> retrievePropertiesAllObjects(
            List<PropertyFilterSpec> listpfs) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

        RetrieveResult rslts =
                vimPort.retrievePropertiesEx(propCollectorRef, listpfs,
                        propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null
                && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts =
                    vimPort.continueRetrievePropertiesEx(propCollectorRef, token);
            token = null;
            if (rslts != null) {
                token = rslts.getToken();
                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcontent.addAll(rslts.getObjects());
                }
            }
        }

        return listobjcontent;
    }

    @Action
    public void run() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        propCollectorRef = serviceContent.getPropertyCollector();
        eventManagerRef = serviceContent.getEventManager();
        System.out.println("EV Man Val " + eventManagerRef.getValue());

        formatLatestEvent();
    }
}