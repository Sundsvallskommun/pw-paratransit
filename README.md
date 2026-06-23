# PwParatransit

<p>Paratransit is a service which integrates with Camunda process engine for starting and updating processes regarding paratransits. It also support processes with business logic and integration to other services.</p>

<h3>Automatic deployment</h3>

<p>The automatic deployment interprets properties present in the application yaml file. The following settings are used to configure the automatic deployment mechanism:</p>

<table class="settings">
	<thead>
		<tr>
			<th>Setting</th>
			<th>Description</th>
			<th>Default&nbsp;value</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td class="code">config.process-engine-base-url</td>
			<td>URL address to the process engine rest API that the external task client polls (sources <span class="code">camunda.bpm.client.base-url</span>). Must point at the same engine as <span class="code">process-engine.type</span></td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">process-engine.deployment</td>
			<td>The node contains information about the processes that shall be deployed (engine-agnostic, deployed via EngineClient)</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">process-engine.deployment.autoDeployEnabled</td>
			<td>When set to <strong>false</strong> then autodeploy is disabled</td>
			<td><strong>true</strong></td>
		</tr>
		<tr>
			<td class="code">process-engine.deployment.processes</td>
			<td>When deployment node is present, the processes node should contain a list<br />
			of one or more processes to deploy (in one or more tenant namespaces)</td>
			<td><strong>emtpy list</strong></td>
		</tr>
	</tbody>
</table>

<p>The following attributes are possible to configure for each entry in the list of processes:</p>

<table class="settings">
	<thead>
		<tr>
			<th>Setting</th>
			<th>Description</th>
			<th>Default&nbsp;value</th>
		</tr>
		<tr>
			<td class="code">name</td>
			<td>Human readable name of the process, must not be null or empty</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">tenant</td>
			<td>
				The tenant id that owns the process which will affect in which namespace the process will be deployed.<br />
				If no id is present, the process will be deployed to the default namespace (making it a shared process, usable<br />
				for all tenants in Camunda)
			</td>
			<td><strong>null</strong></td>
		</tr>
		<tr>
			<td class="code">bpmnResourcePattern</td>
			<td>
				Pattern to match when searching for bpmn resources in the service.<br />
				For example&nbsp;<span class="code">classpath*:processmodels/*.bpmn</span>
			</td>
			<td><strong>classpath*:**/*.bpmn</strong></td>
		</tr>
		<tr>
			<td class="code">dmnResourcePattern</td>
			<td>
				Pattern to match when searching for dmn resources in the service.<br />
				For example&nbsp;<span class="code">classpath*:processmodels/*.dmn</span>
			</td>
			<td><strong>classpath*:**/*.dmn</strong></td>
		</tr>
		<tr>
			<td class="code">formResourcePattern</td>
			<td>
				Pattern to match when searching for form resources in the service.<br />
				For example&nbsp;<span class="code">classpath*:processmodels/*.form</span>
			</td>
			<td><strong>classpath*:**/*.form</strong></td>
		</tr>
	</thead>
</table>

<p>Below is an example definition for a single process for tenant id "my_namespace" with defined process models in the awesome directory:</p>

<table class="settings">
	<tbody>
		<tr>
			<th>
				Example
			</th>
		</tr>
		<tr>
			<td class="code">
			<span class="code">
				&nbsp; process-engine:<br />
				&nbsp; &nbsp; type: operaton<br />
				&nbsp; &nbsp; deployment:<br />
				&nbsp; &nbsp; &nbsp; processes:<br />
				&nbsp; &nbsp; &nbsp; &nbsp; - name: My awesome process<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; tenant: my_namespace<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; bpmnResourcePattern: classpath*:processmodels/awesome/*.bpmn<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; dmnResourcePattern: classpath*:processmodels/awesome/*.dmn<br />
				&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; formResourcePattern: classpath*:processmodels/awesome/*.form
			</span>
			</td>
		</tr>
	</tbody>
</table>

<h3>Running as dual instances (Camunda + Operaton)</h3>

<p>The service is migrating from Camunda to <strong>Operaton</strong> as its process engine using a dual-engine
strategy with a gradual transition:</p>

<ul>
	<li><strong>New processes</strong> are always started in Operaton (<span class="code">ProcessService.startProcess</span> always targets the <span class="code">OperatonClient</span>).</li>
	<li><strong>Older processes</strong> keep living in Camunda; updates probe Operaton first and fall back to Camunda.</li>
	<li>The <span class="code">process-engine.type</span> property (<span class="code">camunda</span> | <span class="code">operaton</span>) controls which engine <em>a single instance</em> deploys to and polls.</li>
</ul>

<p>During the transition the <strong>same artifact</strong> is deployed as <strong>two instances</strong> that differ only in
configuration:</p>

<table class="settings">
	<thead>
		<tr>
			<th>Instance</th>
			<th>Configuration</th>
			<th>Role</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td><strong>Camunda instance</strong></td>
			<td class="code">config.process-engine-type=camunda<br />config.process-engine-base-url=&lt;camunda engine-rest&gt;</td>
			<td>Drains and finishes the older processes that still live in Camunda</td>
		</tr>
		<tr>
			<td><strong>Operaton instance</strong></td>
			<td class="code">config.process-engine-type=operaton<br />config.process-engine-base-url=&lt;operaton engine-rest&gt;</td>
			<td>Runs all new processes</td>
		</tr>
	</tbody>
</table>

<p>Notes:</p>

<ul>
	<li><strong>Both</strong> instances require <span class="code">config.operaton.*</span> (base-url / client-id / client-secret / token-uri), because the <span class="code">OperatonClient</span> bean and the <span class="code">operaton</span> OAuth2 registration always load regardless of engine type.</li>
	<li><span class="code">config.process-engine-base-url</span> must point at the <strong>same</strong> engine as <span class="code">config.process-engine-type</span>, so that deploy/start and external-task polling hit the same engine.</li>
	<li><span class="code">config.process-engine-base-url</span> and <span class="code">config.operaton.*</span> have <strong>no defaults</strong> by design and must be provisioned per environment, otherwise the app will not boot.</li>
</ul>

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-paratransit&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-paratransit)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-paratransit&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-paratransit)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-paratransit&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-paratransit)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-paratransit&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-paratransit)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-paratransit&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-paratransit)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_pw-paratransit&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_pw-paratransit)

## 

Copyright (c) 2025 Sundsvalls kommun
