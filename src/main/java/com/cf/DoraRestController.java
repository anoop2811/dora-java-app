package com.cf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.QueryParam;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import antlr.collections.impl.IntRange;

@RestController
public class DoraRestController {

	@Autowired
	private UserDao userDao;

	@RequestMapping("/")
	public String greeting() {
		return "Hello Dora";
	}

	@RequestMapping(path = "/largetext/{kbytes}", method = RequestMethod.GET)
	public void largeText(@PathVariable int kbytes, HttpServletResponse response) throws IOException {
		response.addHeader("Content-disposition", "attachment;filename=sample.txt");
		response.setContentType("txt/plain");
		StringBuilder sb = new StringBuilder();
		if (kbytes > 5 * 1024) {
			kbytes = 5 * 1024;
		}
		IntStream.range(0, kbytes).parallel().forEach(nbr -> sb.append("1"));
		response.getOutputStream().write(sb.toString().getBytes());
		response.flushBuffer();
	}

	@RequestMapping(path = "/sleep/{sleepTimeStr}", method = RequestMethod.GET)
	public void sleep(@PathVariable String sleepTimeStr, HttpServletResponse response)
			throws IOException, InterruptedException {
		int sleepTime = 1000;
		response.setContentType("text/html;charset=utf-8");
		try {
			sleepTime = Integer.parseInt(sleepTimeStr);
		} catch (Exception e) {
			response.getOutputStream().write(
					"Have defaulted sleep time to 1 second. Please give me correct instructions next time".getBytes());
		}

		response.getOutputStream()
				.write(("<ul><li>Shutting eyes for " + sleepTime / 1000 + " seconds only....</li></br>").getBytes());
		if (sleepTime <= 0) {
			sleepTime = 1000;
		}
		response.flushBuffer();
		Thread.sleep(sleepTime);
		response.getOutputStream()
				.write("<li>That was a wonderful nap. Awake and ready for orders!!!</li></ul>".getBytes());

	}

	@RequestMapping(path = "/find/{fileName}", method = RequestMethod.GET)
	public String findFile(@PathVariable String fileName, HttpServletResponse response)
			throws ExecuteException, IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (!StringUtils.isEmpty(fileName)) {
			CommandLine cmd = new CommandLine("find");
			cmd.addArgument("/");
			cmd.addArgument("-name");
			cmd.addArgument(fileName);
			DefaultExecutor executor = new DefaultExecutor();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
			executor.setStreamHandler(streamHandler);
			executor.setWatchdog(watchdog);
			executor.execute(cmd);

		}
		return (outputStream.toString());
	}

	@RequestMapping("/ping")
	public String ping(@RequestParam(required = false, name = "address") String address,
			@RequestParam(required = false, name = "maxPing") String maxPing, HttpServletResponse response)
					throws ExecuteException, IOException {
		response.setContentType("text/html;charset=utf-8");
		if (StringUtils.isEmpty(address)) {
			return "PONG";
		} else {
			CommandLine cmd = new CommandLine("ping");
			cmd.addArgument("-c");
			cmd.addArgument(maxPing);
			cmd.addArgument(address);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			DefaultExecutor executor = new DefaultExecutor();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			ExecuteWatchdog watchdog = new ExecuteWatchdog(50000);
			executor.setStreamHandler(streamHandler);
			executor.setWatchdog(watchdog);
			executor.execute(cmd);
			return ("<pre>" + outputStream.toString() + "</pre>");
		}

	}

	@RequestMapping("/create")
	@ResponseBody
	public String create(String email, String name) {
		User user = null;
		try {
			user = new User(email, name);
			userDao.save(user);
		} catch (Exception ex) {
			return "Error creating the user: " + ex.toString();
		}
		return "User succesfully created! (id = " + user.getId() + ")";
	}

	@RequestMapping("/delete")
	@ResponseBody
	public String delete(long id) {
		try {
			User user = new User(id);
			userDao.delete(user);
		} catch (Exception ex) {
			return "Error deleting the user:" + ex.toString();
		}
		return "User succesfully deleted!";
	}

	@RequestMapping("/get-by-email")
	@ResponseBody
	public String getByEmail(String email) {
		String userId;
		try {
			User user = userDao.findByEmail(email);
			userId = String.valueOf(user.getId());
		} catch (Exception ex) {
			return "User not found";
		}
		return "The user id is: " + userId;
	}

	@RequestMapping("/show-all")
	@ResponseBody
	public String getAll() {
		StringBuilder userStr = new StringBuilder();
		try {
			userDao.findAll().forEach(user -> {
				userStr.append(String.valueOf(user));
			});
		} catch (Exception ex) {
			return "User not found, exception: " + ex.getMessage();
		}
		return userStr.toString();
	}

	@RequestMapping("/repeat-query")
	public void runQueriesForTime(int repeatTimes, HttpServletResponse response) {
		response.setContentType("text/html;charset=utf-8");
		try {

			for (int i = 0; i < repeatTimes; i++) {

				response.getOutputStream().write(getAll().getBytes());
				response.getOutputStream().write("</br>".getBytes());
				response.getOutputStream().write("==================".getBytes());
				response.getOutputStream().write("</br>".getBytes());
				response.flushBuffer();
				Thread.sleep(1000);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@RequestMapping("/update")
	@ResponseBody
	public String updateUser(long id, String email, String name) {
		try {
			User user = userDao.findOne(id);
			user.setEmail(email);
			user.setName(name);
			userDao.save(user);
		} catch (Exception ex) {
			return "Error updating the user: " + ex.toString();
		}
		return "User succesfully updated!";
	}

	@RequestMapping(path = "/tcpdump", method = RequestMethod.GET)
	public void tcpdump(@RequestParam(value = "packets", required = false) Integer packets,
			@RequestParam(value = "host") String host,
			@RequestParam(value = "retries", defaultValue = "1") Integer retries, HttpServletResponse response)
					throws ExecuteException, IOException, InterruptedException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (packets == null || packets <= 0) {
			packets = 10;
		}
		CommandLine cmd = new CommandLine("sudo");
		cmd.addArgument("tcpdump");
		cmd.addArgument("-vv");
		cmd.addArgument("-c");
		cmd.addArgument(String.valueOf(packets));
		cmd.addArgument("host");
		cmd.addArgument(host);
		response.setContentType("text/html;charset=utf-8");
		DefaultExecutor executor = new DefaultExecutor();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
		executor.setStreamHandler(streamHandler);
		executor.setWatchdog(watchdog);
		//for (int i = 0; i < retries; i++) {
			try {
				executor.execute(cmd);
			} catch (Exception e) {
				response.getOutputStream().write(("<pre>" + Arrays.toString(e.getStackTrace()) + "</pre>").getBytes());
			}
			String output = outputStream.toString().replace("ICMP", "<strong><font color=\"red\">ICMP</font></strong>");
			output = output.replace("ESP", "<strong><font color=\"red\">ESP</font></strong>");
			response.getOutputStream().write(("<pre>" + output + "</pre>").getBytes());
		//	Thread.sleep(1000);
		//}
	}

}
